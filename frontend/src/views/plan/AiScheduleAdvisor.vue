<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { BulbOutlined, CheckCircleOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
import {
  applyAiScheduleSuggestion,
  getAiScheduleRecord,
  getAiScheduleSuggestions,
  listAiScheduleRecords,
  type AiScheduleOption,
  type AiScheduleRecordSummary,
  type AiScheduleSuggestion,
} from '@/api/ai/decision'
import { fetchPlans } from '@/api/plan/plan'
import type { IdValue, ProductionPlanItem } from '@/types/domain'

const loading = ref(false)
const applying = ref<string>()
const plans = ref<ProductionPlanItem[]>([])
const selectedPlanId = ref<IdValue>()
const suggestion = ref<AiScheduleSuggestion>()
const records = ref<AiScheduleRecordSummary[]>([])
const selectedRecordId = ref<IdValue>()

const planOptions = computed(() =>
  plans.value.map((item) => ({
    value: item.planId,
    label: `${item.orderNo || item.planId} · ${item.batchNo || item.batchId} · ${item.routeName || '未命名路线'}`,
  })),
)

const recordOptions = computed(() =>
  records.value.map((item) => ({
    value: item.recordId,
    label: `${item.createTime} · ${item.orderNo || item.planId} · ${item.status === 'APPLIED' ? `已应用 ${item.selectedStrategy}` : '待确认'}`,
  })),
)

const strategyTone: Record<string, string> = {
  DELIVERY: 'amber',
  UTILIZATION: 'teal',
  COST: 'slate',
}

const stepColumns = [
  { title: '工序', dataIndex: 'stepName', key: 'stepName', width: 110 },
  { title: '推荐机台', key: 'machine', width: 170 },
  { title: '预测工时', dataIndex: 'predictedHours', key: 'predictedHours', width: 90 },
  { title: '开始', dataIndex: 'startTime', key: 'startTime', width: 165 },
  { title: '结束', dataIndex: 'endTime', key: 'endTime', width: 165 },
  { title: '推荐依据', dataIndex: 'recommendationReason', key: 'recommendationReason' },
]

const loadPlans = async () => {
  const response = await fetchPlans({ pageNum: 1, pageSize: 200 })
  plans.value = response.list.filter((item) => !['COMPLETED', 'CANCELLED'].includes(item.status))
  selectedPlanId.value ||= plans.value[0]?.planId
}

const loadHistory = async () => {
  const response = await listAiScheduleRecords()
  records.value = response.data
  const latestRecord = records.value[0]
  if (!selectedRecordId.value && latestRecord) {
    await restoreRecord(latestRecord.recordId)
  }
}

const restoreRecord = async (recordId: IdValue) => {
  loading.value = true
  try {
    const response = await getAiScheduleRecord(recordId)
    suggestion.value = response.data
    selectedRecordId.value = recordId
    selectedPlanId.value = response.data.planId
  } finally {
    loading.value = false
  }
}

const generate = async () => {
  if (!selectedPlanId.value) {
    message.warning('请先选择生产计划')
    return
  }
  loading.value = true
  try {
    const response = await getAiScheduleSuggestions(selectedPlanId.value)
    suggestion.value = response.data
    selectedRecordId.value = response.data.recordId
    await loadHistory()
  } finally {
    loading.value = false
  }
}

const confirmApply = (option: AiScheduleOption) => {
  Modal.confirm({
    title: `确认应用“${option.strategyName}”方案？`,
    content: '该操作会更新工序机台、开始时间、结束时间和预测工时，并写入人工确认审计记录。',
    okText: '确认应用',
    cancelText: '继续比较',
    onOk: async () => {
      if (!selectedPlanId.value) return
      applying.value = option.strategy
      try {
        await applyAiScheduleSuggestion(selectedPlanId.value, {
          recordId: suggestion.value?.recordId,
          strategy: option.strategy,
          confirmationRemark: '由 AI 智能排产建议页面人工确认',
        })
        message.success('排产方案已应用并落库')
        if (suggestion.value?.recordId) {
          await restoreRecord(suggestion.value.recordId)
        }
        await loadHistory()
      } finally {
        applying.value = undefined
      }
    },
  })
}

const hours = (minutes?: number) => `${Math.round(((minutes || 0) / 60) * 10) / 10}h`

onMounted(async () => {
  await loadPlans()
  await loadHistory()
})
</script>

<template>
  <main class="advisor-page">
    <section class="hero">
      <div>
        <p class="eyebrow">OR-TOOLS × DEEPSEEK</p>
        <h1>智能排产建议台</h1>
        <p class="hero-copy">算法负责把计划排出来，大模型负责把利弊说清楚。最终按钮，仍在人手里。</p>
      </div>
      <div class="engine-stamp">
        <ThunderboltOutlined />
        <span>决策引擎</span>
        <strong>CP-SAT</strong>
      </div>
    </section>

    <section class="control-strip">
      <div class="plan-picker">
        <span>选择待优化计划</span>
        <a-select
          v-model:value="selectedPlanId"
          show-search
          :options="planOptions"
          :filter-option="(input: string, option: any) => option.label.toLowerCase().includes(input.toLowerCase())"
          placeholder="订单 / 批次 / 工艺路线"
        />
      </div>
      <a-button type="primary" size="large" :loading="loading" @click="generate">
        <template #icon><BulbOutlined /></template>
        生成三套方案
      </a-button>
    </section>

    <section v-if="records.length" class="history-strip">
      <div>
        <span>历史方案记录</span>
        <a-select
          v-model:value="selectedRecordId"
          :options="recordOptions"
          placeholder="选择历史记录"
          @change="restoreRecord"
        />
      </div>
      <a-tag :color="suggestion?.recordStatus === 'APPLIED' ? 'success' : 'gold'">
        {{ suggestion?.recordStatus === 'APPLIED' ? `已应用 ${suggestion.selectedStrategy}` : '已保存 · 待人工确认' }}
      </a-tag>
    </section>

    <a-alert
      v-if="suggestion"
      class="decision-notice"
      :message="suggestion.notice"
      type="warning"
      show-icon
    />

    <a-spin :spinning="loading">
      <section v-if="suggestion" class="option-grid">
        <article
          v-for="option in suggestion.options"
          :key="option.strategy"
          class="option-card"
          :class="`tone-${strategyTone[option.strategy]}`"
        >
          <header>
            <div>
              <p>{{ option.strategy }}</p>
              <h2>{{ option.strategyName }}</h2>
            </div>
            <a-tag :color="option.solverStatus === 'OPTIMAL' ? 'success' : 'processing'">
              {{ option.solverStatus }}
            </a-tag>
          </header>

          <div class="metric-row">
            <div><span>总历时</span><strong>{{ hours(option.totalMinutes) }}</strong></div>
            <div><span>预计延期</span><strong>{{ hours(option.delayMinutes) }}</strong></div>
            <div><span>机台切换</span><strong>{{ option.machineChanges }}</strong></div>
            <div><span>利用率分</span><strong>{{ option.estimatedUtilizationScore }}</strong></div>
          </div>

          <div class="explanation">
            <span class="model-chip">{{ option.aiGenerated ? option.model : '规则降级报告' }}</span>
            <p>{{ option.explanation }}</p>
          </div>

          <a-table
            size="small"
            :columns="stepColumns"
            :data-source="option.steps"
            :pagination="false"
            :scroll="{ x: 880 }"
            row-key="planStepId"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'machine'">
                <strong>{{ record.machineCode }}</strong>
                <span class="machine-name">{{ record.machineName }}</span>
              </template>
              <template v-else-if="column.key === 'predictedHours'">{{ record.predictedHours }}h</template>
            </template>
          </a-table>

          <footer>
            <span>交期：{{ suggestion.deliveryDate || '未设置' }}</span>
            <a-button
              type="primary"
              :loading="applying === option.strategy"
              :disabled="!['OPTIMAL', 'FEASIBLE'].includes(option.solverStatus)"
              @click="confirmApply(option)"
            >
              <template #icon><CheckCircleOutlined /></template>
              人工确认并应用
            </a-button>
          </footer>
        </article>
      </section>

      <a-empty v-else class="empty-state" description="选择计划后生成交期、利用率、成本三类排产方案" />
    </a-spin>
  </main>
</template>

<style scoped>
.advisor-page {
  --ink: #152019;
  --paper: #f2efe7;
  min-height: calc(100vh - 108px);
  padding: 22px;
  color: var(--ink);
  background:
    linear-gradient(rgba(21, 32, 25, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(21, 32, 25, 0.035) 1px, transparent 1px),
    var(--paper);
  background-size: 24px 24px;
}

.hero, .control-strip, .history-strip, .option-card {
  border: 1px solid rgba(21, 32, 25, 0.14);
  box-shadow: 0 14px 36px rgba(46, 42, 31, 0.08);
}

.hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding: 32px;
  background: #17251d;
  color: #f5f1e7;
}

.eyebrow, .option-card header p {
  margin: 0 0 8px;
  color: #d4a94f;
  font: 700 12px/1.2 Consolas, monospace;
  letter-spacing: .16em;
}

h1 { margin: 0; font: 800 clamp(28px, 4vw, 50px)/1.04 Georgia, serif; }
.hero-copy { max-width: 650px; margin: 14px 0 0; color: #c8d0c9; font-size: 15px; }
.engine-stamp { display: grid; min-width: 130px; padding: 18px; border: 1px solid #506257; text-align: right; }
.engine-stamp :deep(svg) { margin-left: auto; color: #d4a94f; font-size: 22px; }
.engine-stamp span { margin-top: 12px; color: #9daba1; font-size: 12px; }
.engine-stamp strong { font: 800 24px/1.1 Georgia, serif; }

.control-strip {
  display: flex;
  align-items: end;
  gap: 16px;
  margin: 16px 0;
  padding: 18px;
  background: rgba(255, 253, 247, .92);
}

.plan-picker { display: grid; flex: 1; gap: 8px; }
.plan-picker > span { color: #536059; font-size: 12px; font-weight: 700; letter-spacing: .06em; }
.plan-picker :deep(.ant-select) { width: 100%; }
.decision-notice { margin-bottom: 16px; }
.history-strip {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 16px;
  margin: -2px 0 16px;
  padding: 14px 18px;
  background: rgba(255, 253, 247, .92);
}
.history-strip > div { display: grid; flex: 1; gap: 7px; }
.history-strip span { color: #536059; font-size: 12px; font-weight: 700; }
.history-strip :deep(.ant-select) { width: 100%; }
.option-grid { display: grid; gap: 18px; }

.option-card {
  overflow: hidden;
  background: #fffdf7;
  border-top-width: 5px;
}
.tone-amber { border-top-color: #c88924; }
.tone-teal { border-top-color: #2c7a6b; }
.tone-slate { border-top-color: #58636d; }
.option-card header, .option-card footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 22px;
}
.option-card h2 { margin: 0; font: 800 26px/1.1 Georgia, serif; }
.metric-row { display: grid; grid-template-columns: repeat(4, 1fr); border-block: 1px solid #e4dfd3; }
.metric-row div { padding: 15px 22px; border-right: 1px solid #e4dfd3; }
.metric-row div:last-child { border-right: 0; }
.metric-row span, .machine-name { display: block; color: #748078; font-size: 12px; }
.metric-row strong { display: block; margin-top: 5px; font: 800 22px/1 Georgia, serif; }
.explanation { padding: 18px 22px; background: #f6f2e8; }
.explanation p { margin: 10px 0 0; line-height: 1.75; }
.model-chip { display: inline-block; padding: 3px 8px; border: 1px solid #c7beaa; color: #5b625d; font: 11px Consolas, monospace; }
.option-card :deep(.ant-table-wrapper) { margin: 0 22px; }
.option-card footer { color: #68736c; font-size: 13px; }
.machine-name { margin-top: 3px; }
.empty-state { padding: 90px 0; }

@media (max-width: 760px) {
  .advisor-page { padding: 12px; }
  .hero, .control-strip, .history-strip, .option-card header, .option-card footer { align-items: stretch; flex-direction: column; }
  .metric-row { grid-template-columns: repeat(2, 1fr); }
  .metric-row div:nth-child(2) { border-right: 0; }
}
</style>
