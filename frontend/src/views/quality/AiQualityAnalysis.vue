<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ExperimentOutlined, SafetyCertificateOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { analyzeQcRecord, type AiQualityAnalysis } from '@/api/ai/decision'
import { fetchQcRecords } from '@/api/quality/qcRecord'
import type { IdValue, QcRecordItem } from '@/types/domain'
import { formatInspectionId } from '@/utils/idFormat'

const records = ref<QcRecordItem[]>([])
const selectedId = ref<IdValue>()
const loading = ref(false)
const analysis = ref<AiQualityAnalysis>()

const recordOptions = computed(() =>
  records.value.map((item) => ({
    value: item.inspectionId,
    label: `${formatInspectionId(item.inspectionId)} · ${item.inspectTime} · ${item.resultJudge} · ${item.resultValue || '无缺陷'}`,
  })),
)

const riskColor = computed(() => ({ HIGH: 'error', MEDIUM: 'warning', LOW: 'success' }[analysis.value?.riskLevel || 'LOW']))

const loadRecords = async () => {
  const response = await fetchQcRecords({ pageNum: 1, pageSize: 100 })
  records.value = response.list
  selectedId.value ||= records.value[0]?.inspectionId
}

const runAnalysis = async () => {
  if (!selectedId.value) {
    message.warning('请先选择质检记录')
    return
  }
  loading.value = true
  try {
    analysis.value = (await analyzeQcRecord(selectedId.value)).data
  } finally {
    loading.value = false
  }
}

onMounted(loadRecords)
</script>

<template>
  <main class="quality-lab">
    <section class="lab-header">
      <div>
        <p>QUALITY INTELLIGENCE / 质量情报</p>
        <h1>质检结果分析室</h1>
        <span>ONNX 提供检测事实，趋势规则寻找信号，DeepSeek 负责生成可读报告。</span>
      </div>
      <ExperimentOutlined />
    </section>

    <section class="selector-panel">
      <a-select
        v-model:value="selectedId"
        show-search
        :options="recordOptions"
        :filter-option="(input: string, option: any) => option.label.toLowerCase().includes(input.toLowerCase())"
        placeholder="选择一条质检记录"
      />
      <a-button type="primary" size="large" :loading="loading" @click="runAnalysis">生成分析报告</a-button>
    </section>

    <a-spin :spinning="loading">
      <template v-if="analysis">
        <section class="signal-board">
          <div class="risk-cell">
            <span>综合风险</span>
            <a-tag :color="riskColor">{{ analysis.riskLevel }}</a-tag>
          </div>
          <div><span>检测结论</span><strong>{{ analysis.resultJudge }}</strong></div>
          <div><span>缺陷类型</span><strong>{{ analysis.defectType }}</strong></div>
          <div><span>严重程度</span><strong>{{ analysis.severity }}</strong></div>
          <div><span>近期样本</span><strong>{{ analysis.recentSampleCount }}</strong></div>
          <div><span>近期异常率</span><strong>{{ analysis.recentDefectRate }}%</strong></div>
        </section>

        <section class="analysis-grid">
          <article class="report-sheet">
            <div class="sheet-title">
              <SafetyCertificateOutlined />
              <div>
                <span>{{ analysis.aiGenerated ? analysis.model : '规则降级报告' }}</span>
                <h2>分析结论</h2>
              </div>
            </div>
            <p>{{ analysis.report }}</p>
            <a-alert :message="analysis.notice" type="info" show-icon />
          </article>

          <article class="evidence-card cause">
            <span class="index">01</span>
            <h2>可能原因</h2>
            <ul><li v-for="item in analysis.possibleCauses" :key="item">{{ item }}</li></ul>
          </article>

          <article class="evidence-card action">
            <span class="index">02</span>
            <h2>建议动作</h2>
            <ol><li v-for="item in analysis.recommendedActions" :key="item">{{ item }}</li></ol>
          </article>
        </section>
      </template>
      <a-empty v-else class="empty-state" description="选择质检记录，生成风险、趋势与根因辅助分析" />
    </a-spin>
  </main>
</template>

<style scoped>
.quality-lab {
  min-height: calc(100vh - 108px);
  padding: 24px;
  color: #18212b;
  background: #edf0ed;
}
.lab-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32px;
  color: #eef5ef;
  background:
    radial-gradient(circle at 80% 10%, rgba(117, 176, 145, .22), transparent 32%),
    #14251f;
  border-radius: 4px;
}
.lab-header p { margin: 0 0 10px; color: #8eb99f; font: 700 12px Consolas, monospace; letter-spacing: .14em; }
.lab-header h1 { margin: 0 0 10px; font: 800 clamp(28px, 4vw, 48px)/1 Georgia, serif; }
.lab-header span { color: #b9c9c0; }
.lab-header > :deep(svg) { color: #8eb99f; font-size: 72px; opacity: .7; }
.selector-panel {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
  margin: 14px 0;
  padding: 16px;
  background: #fff;
  border: 1px solid #d9ded9;
}
.signal-board {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  background: #fff;
  border: 1px solid #d9ded9;
}
.signal-board > div { min-height: 88px; padding: 18px; border-right: 1px solid #d9ded9; }
.signal-board > div:last-child { border-right: 0; }
.signal-board span { display: block; margin-bottom: 12px; color: #778179; font-size: 12px; }
.signal-board strong { font: 800 22px Georgia, serif; }
.risk-cell :deep(.ant-tag) { padding: 4px 10px; font-weight: 800; }
.analysis-grid { display: grid; grid-template-columns: 1.1fr .9fr; gap: 14px; margin-top: 14px; }
.report-sheet, .evidence-card { padding: 26px; background: #fff; border: 1px solid #d9ded9; }
.report-sheet { grid-row: span 2; }
.sheet-title { display: flex; align-items: center; gap: 14px; padding-bottom: 18px; border-bottom: 2px solid #1e3a30; }
.sheet-title :deep(svg) { color: #36725d; font-size: 28px; }
.sheet-title span { color: #7a857e; font: 11px Consolas, monospace; }
.sheet-title h2, .evidence-card h2 { margin: 4px 0 0; font: 800 24px Georgia, serif; }
.report-sheet > p { min-height: 180px; margin: 24px 0; font-size: 15px; line-height: 1.9; white-space: pre-line; }
.evidence-card { position: relative; overflow: hidden; }
.evidence-card .index { position: absolute; right: 18px; top: 10px; color: rgba(28, 57, 47, .09); font: 800 68px Georgia, serif; }
.evidence-card ul, .evidence-card ol { position: relative; margin: 18px 0 0; padding-left: 20px; }
.evidence-card li { margin: 10px 0; line-height: 1.65; }
.cause { border-left: 5px solid #be7d32; }
.action { border-left: 5px solid #36725d; }
.empty-state { padding: 100px 0; }
@media (max-width: 900px) {
  .quality-lab { padding: 12px; }
  .selector-panel, .analysis-grid { grid-template-columns: 1fr; }
  .signal-board { grid-template-columns: repeat(2, 1fr); }
  .signal-board > div { border-bottom: 1px solid #d9ded9; }
  .report-sheet { grid-row: auto; }
}
</style>
