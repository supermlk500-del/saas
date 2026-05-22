<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import TablePage from '@/components/TablePage.vue'
import FormModal from '@/components/FormModal.vue'
import SearchBar from '@/components/SearchBar.vue'
import ActionBar from '@/components/ActionBar.vue'
import { useTable } from '@/hooks/useTable'
import { useScheduleStore, type PlanRecord } from '@/stores/schedule'

const searchForm = reactive({
  planNo: '',
  orderNo: '',
})

const searchFields = [
  { label: '计划号', name: 'planNo', type: 'input', placeholder: '请输入计划号', width: '200px' },
  { label: '订单号', name: 'orderNo', type: 'input', placeholder: '请输入订单号', width: '200px' },
]

const columns = [
  { title: '计划号', dataIndex: 'planNo', key: 'planNo' },
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo' },
  { title: '车间', dataIndex: 'line', key: 'line' },
  { title: '开始日期', dataIndex: 'startDate', key: 'startDate' },
  { title: '状态', dataIndex: 'status', key: 'status' },
]

const { data, loading, pagination } = useTable<PlanRecord>()
const scheduleStore = useScheduleStore()

const modalOpen = ref(false)
const formModel = reactive({
  orderNo: '',
  line: '',
  startDate: '',
  fabric: '',
  qty: '',
  priority: 'Normal',
})
const rules = {
  orderNo: [{ required: true, message: '请输入订单号' }],
  line: [{ required: true, message: '请输入车间' }],
  startDate: [{ required: true, message: '请选择开始日期' }],
}

const formFields = [
  { label: '订单号', name: 'orderNo', type: 'input', placeholder: '请输入订单号' },
  { label: '车间', name: 'line', type: 'input', placeholder: '如 缝纫车间' },
  { label: '开始日期', name: 'startDate', type: 'input', placeholder: '如 2026-04-09' },
  { label: '布种', name: 'fabric', type: 'input', placeholder: '如 全棉 32S 平纹' },
  { label: '数量', name: 'qty', type: 'input', placeholder: '如 5000m' },
  {
    label: '优先级',
    name: 'priority',
    type: 'select',
    options: [
      { label: 'High', value: 'High' },
      { label: 'Normal', value: 'Normal' },
      { label: 'Low', value: 'Low' },
    ],
  },
]

const actionBar = [
  { key: 'generate', label: '插入排产队列', type: 'primary' },
  { key: 'ai-assist', label: 'AI辅助排产' },
  { key: 'strategy', label: '排产策略' },
]

const aiVisible = ref(false)
const chatInput = ref('')
const chatMessages = ref<{ role: 'ai' | 'user'; text: string }[]>([])

const aiInsight = computed(() => {
  const plans = scheduleStore.plans
  const pending = scheduleStore.pendingOrders
  const schedulingCount = plans.filter((item) => item.status === '排产中').length
  const queuedCount = plans.filter((item) => item.status === '待排产').length
  const doneCount = plans.filter((item) => item.status === '已生成').length
  const highPending = pending.filter((item) => item.priority === 'High').length

  const startDates = plans.map((item) => item.startDate).filter((x): x is string => !!x).sort()

  return {
    totalPlans: plans.length,
    schedulingCount,
    queuedCount,
    doneCount,
    pendingCount: pending.length,
    highPending,
    earliestStart: startDates[0] ?? '--',
    latestStart: startDates[startDates.length - 1] ?? '--',
  }
})

const aiSuggestions = computed(() => {
  const tips: string[] = []
  const insight = aiInsight.value

  if (insight.highPending > 0) {
    tips.push(`当前有 ${insight.highPending} 个高优先级订单待排，建议优先在早班窗口锁定机台。`)
  }

  if (insight.queuedCount > 0) {
    tips.push(`当前有 ${insight.queuedCount} 条计划处于“待排产”，建议先在甘特图执行一次自动排产。`)
  }

  if (insight.schedulingCount >= 3) {
    tips.push('排产中任务较多，建议在中班前复核瓶颈工序，预留换线缓冲。')
  }

  if (tips.length === 0) {
    tips.push('当前排产负荷平稳，建议按计划执行并在班前会关注异常订单。')
  }

  tips.push('若出现交期冲突，可先提升高优先级订单并拆分低优先级大单。')
  return tips
})

const parseQty = (qty: string) => Number(qty.replace(/[^\d.]/g, '')) || 0

const topPendingOrders = computed(() => {
  const weight = { High: 3, Normal: 2, Low: 1 }
  return [...scheduleStore.pendingOrders]
    .sort((a, b) => {
      const byPriority = weight[b.priority] - weight[a.priority]
      if (byPriority !== 0) return byPriority
      return parseQty(b.qty) - parseQty(a.qty)
    })
    .slice(0, 3)
})

const lineLoadSummary = computed(() => {
  const map = new Map<string, number>()
  for (const plan of scheduleStore.plans) {
    if (!plan.line) continue
    map.set(plan.line, (map.get(plan.line) ?? 0) + 1)
  }
  return [...map.entries()]
    .sort((a, b) => b[1] - a[1])
    .slice(0, 3)
    .map(([line, count]) => `${line}(${count})`)
})

const latestQueuedPlans = computed(() => {
  return scheduleStore.plans
    .filter((plan) => plan.status === '待排产')
    .slice(0, 3)
    .map((plan) => `${plan.orderNo}/${plan.startDate || '--'}`)
})

const buildAiReply = (question: string) => {
  const q = question.trim()
  if (!q) return '可以具体说下你的排产问题，我会结合当前计划给建议。'

  const topOrdersText =
    topPendingOrders.value.length > 0
      ? topPendingOrders.value.map((item) => `${item.id}(${item.priority}, ${item.qty})`).join('、')
      : '暂无待排订单'

  const lineLoadText =
    lineLoadSummary.value.length > 0
      ? lineLoadSummary.value.join('、')
      : '暂无车间负荷数据'

  const queuedText =
    latestQueuedPlans.value.length > 0
      ? latestQueuedPlans.value.join('、')
      : '当前无待排产计划'

  if (q.includes('先排') || q.includes('哪几单')) {
    return `建议先排这几单：${topOrdersText}。优先规则已按优先级和数量综合排序，可先在早班窗口锁定。`
  }

  if (q.includes('高优先级') || q.includes('优先')) {
    return `当前高优先级待排 ${aiInsight.value.highPending} 单。建议优先处理：${topOrdersText}。同时在高负荷车间预留半班换线缓冲。`
  }

  if (q.includes('交期') || q.includes('延期')) {
    return `待排产计划（订单/开始日）为：${queuedText}。建议先锁定最临近开始日订单，再拆分低优先级大单到后续班次，降低延期风险。`
  }

  if (q.includes('机台') || q.includes('产线') || q.includes('车间')) {
    return `当前车间计划负荷：${lineLoadText}。建议把连续工艺放在同一车间，瓶颈机台优先分配短单提升周转。`
  }

  if (q.includes('怎么排') || q.includes('排产规则')) {
    return `当前策略是优先级优先、同优先级按数量排序、分配到最早可开工机台。当前建议优先序列：${topOrdersText}。`
  }

  if (q.includes('总结') || q.includes('概览') || q.includes('当前情况')) {
    return `当前共有 ${aiInsight.value.totalPlans} 条计划，待排订单 ${aiInsight.value.pendingCount} 条；高优先级待排 ${aiInsight.value.highPending} 条。优先建议：${topOrdersText}。`
  }

  return '已收到。建议先执行一次自动排产，再根据高优先级与交期冲突做二次微调。'
}

const openAiAssist = () => {
  aiVisible.value = true
  if (chatMessages.value.length === 0) {
    chatMessages.value.push({
      role: 'ai',
      text: `我是排产助手。当前共有 ${aiInsight.value.pendingCount} 个待排订单，建议优先关注：${topPendingOrders.value.map((item) => item.id).join('、') || '暂无'}。你可以问我“先排哪几单”或“如何降低延期风险”。`,
    })
  }
}

const sendChat = () => {
  const text = chatInput.value.trim()
  if (!text) return
  chatMessages.value.push({ role: 'user', text })
  chatMessages.value.push({ role: 'ai', text: buildAiReply(text) })
  chatInput.value = ''
}

const handlePageAction = (key: string) => {
  if (key === 'generate') {
    openModal()
    return
  }
  if (key === 'ai-assist') {
    openAiAssist()
    return
  }
  if (key === 'strategy') {
    message.info('排产策略页面建设中，可先使用 AI 辅助排产查看建议。')
  }
}

const openModal = () => {
  Object.assign(formModel, {
    orderNo: '',
    line: '',
    startDate: '',
    fabric: '',
    qty: '',
    priority: 'Normal',
  })
  modalOpen.value = true
}

const handleOk = async () => {
  if (!formModel.orderNo || !formModel.line || !formModel.startDate) {
    message.warning('请先填写订单号、车间和开始日期')
    return
  }

  scheduleStore.generatePlan({
    orderNo: formModel.orderNo,
    line: formModel.line,
    startDate: formModel.startDate,
    fabric: formModel.fabric || '未指定布种',
    qty: formModel.qty || '3000m',
    priority: (formModel.priority as 'High' | 'Normal' | 'Low') || 'Normal',
  })

  modalOpen.value = false
  message.success('已插入排产队列，可前往生产调度甘特图查看')
  loadData()
}

const loadData = async () => {
  scheduleStore.hydrate()
  loading.value = true
  data.value = scheduleStore.plans.filter((item) => {
    const hitPlan = !searchForm.planNo || item.planNo.includes(searchForm.planNo)
    const hitOrder = !searchForm.orderNo || item.orderNo.includes(searchForm.orderNo)
    return hitPlan && hitOrder
  })
  pagination.total = data.value.length
  loading.value = false
}

const resetSearch = () => {
  searchForm.planNo = ''
  searchForm.orderNo = ''
  loadData()
}

onMounted(loadData)
</script>

<template>
  <TablePage
    title="主生产计划"
    :columns="columns"
    :data="data"
    :loading="loading"
    :pagination="pagination"
  >
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>

  <template #actions>
      <ActionBar :actions="actionBar" @action="handlePageAction" />
    </template>
  </TablePage>

  <FormModal
    v-model:open="modalOpen"
    title="生成计划"
    :model="formModel"
    :rules="rules"
    :fields="formFields"
    @ok="handleOk"
  />

  <a-drawer
    v-model:open="aiVisible"
    title="AI 辅助排产建议"
    placement="right"
    width="460"
  >
    <div class="ai-panel">
      <div class="ai-top">
        <div class="ai-kpis">
          <div class="kpi-item">
            <div class="kpi-label">计划总数</div>
            <div class="kpi-value">{{ aiInsight.totalPlans }}</div>
          </div>
          <div class="kpi-item">
            <div class="kpi-label">待排订单</div>
            <div class="kpi-value">{{ aiInsight.pendingCount }}</div>
          </div>
          <div class="kpi-item">
            <div class="kpi-label">高优先级待排</div>
            <div class="kpi-value">{{ aiInsight.highPending }}</div>
          </div>
        </div>

        <div class="ai-range">计划开始区间：{{ aiInsight.earliestStart }} ~ {{ aiInsight.latestStart }}</div>

        <div class="ai-status">
          <a-tag color="blue">排产中 {{ aiInsight.schedulingCount }}</a-tag>
          <a-tag color="orange">待排产 {{ aiInsight.queuedCount }}</a-tag>
          <a-tag color="green">已生成 {{ aiInsight.doneCount }}</a-tag>
        </div>

        <div class="ai-suggestions-title">排产建议</div>
        <div class="ai-suggestion" v-for="item in aiSuggestions" :key="item">{{ item }}</div>
      </div>

      <div class="ai-chat-section">
        <div class="ai-chat-title">对话交流</div>
        <div class="ai-chat-box">
          <div
            v-for="(msg, idx) in chatMessages"
            :key="`${msg.role}-${idx}`"
            class="chat-row"
            :class="msg.role"
          >
            <div class="chat-bubble">{{ msg.text }}</div>
          </div>
        </div>
        <div class="ai-chat-input">
          <a-input
            v-model:value="chatInput"
            placeholder="例如：先排哪几单更稳妥？"
            @pressEnter="sendChat"
          />
          <a-button type="primary" @click="sendChat">发送</a-button>
        </div>
      </div>
    </div>
  </a-drawer>
</template>

<style scoped>
.ai-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: calc(100vh - 120px);
}

.ai-top {
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
  padding-right: 2px;
}

.ai-kpis {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.kpi-item {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 10px;
}

.kpi-label {
  font-size: 12px;
  color: #64748b;
}

.kpi-value {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.ai-range {
  font-size: 13px;
  color: #334155;
  background: #fff7ed;
  border: 1px solid #fed7aa;
  border-radius: 8px;
  padding: 10px;
}

.ai-status {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.ai-suggestions-title {
  font-weight: 700;
  color: #0f172a;
}

.ai-suggestion {
  font-size: 13px;
  line-height: 1.6;
  color: #334155;
  border-left: 3px solid #ff7a45;
  padding-left: 10px;
}

.ai-chat-title {
  font-weight: 700;
  color: #0f172a;
}

.ai-chat-section {
  margin-top: auto;
  border-top: 1px solid #e2e8f0;
  padding-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ai-chat-box {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 10px;
  background: #f8fafc;
  height: 240px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chat-row {
  display: flex;
}

.chat-row.ai {
  justify-content: flex-start;
}

.chat-row.user {
  justify-content: flex-end;
}

.chat-bubble {
  max-width: 88%;
  padding: 8px 10px;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.5;
}

.chat-row.ai .chat-bubble {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  color: #334155;
}

.chat-row.user .chat-bubble {
  background: #ff7a45;
  color: #ffffff;
}

.ai-chat-input {
  display: flex;
  gap: 8px;
}
</style>
