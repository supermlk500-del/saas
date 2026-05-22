<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'

type NodeConfig = {
  key: string
  name: string
  machineType: string
  stitchLength: string
  stitchType: string
  eta: string
}

const currentProduct = ref('男士衬衫-标准版')

const products = [
  { label: '男士衬衫-标准版', value: '男士衬衫-标准版' },
  { label: '女士衬衫-修身版', value: '女士衬衫-修身版' },
  { label: 'POLO衫-量产版', value: 'POLO衫-量产版' },
]

const libraryNodes: NodeConfig[] = [
  { key: 'inspect', name: '织物检验', machineType: '验布机', stitchLength: '-', stitchType: '来料检验', eta: '12 min' },
  { key: 'cut', name: '裁断', machineType: '自动裁床', stitchLength: '-', stitchType: '刀模裁断', eta: '8 min' },
  { key: 'sew', name: '缝纫', machineType: '平缝机', stitchLength: '3.5mm', stitchType: '40/2 涤纶线', eta: '15 min' },
  { key: 'iron', name: '熨平', machineType: '蒸汽整烫台', stitchLength: '-', stitchType: '整烫定型', eta: '6 min' },
  { key: 'quality', name: '质检', machineType: '人工检台', stitchLength: '-', stitchType: 'AQL 2.5', eta: '10 min' },
  { key: 'pack', name: '包装', machineType: '自动包装机', stitchLength: '-', stitchType: '吊牌+装袋', eta: '7 min' },
]

const routeNodes = ref<NodeConfig[]>([])
const activeNodeKey = ref('')

const activeNode = computed(() => routeNodes.value.find((item) => item.key === activeNodeKey.value) ?? routeNodes.value[0])

const draftConfig = reactive<NodeConfig>({
  key: '',
  name: '',
  machineType: '',
  stitchLength: '',
  stitchType: '',
  eta: '',
})

const storageKey = computed(() => `process_route:${currentProduct.value}`)

const cloneNode = (n: NodeConfig): NodeConfig => ({ ...n })

const ensureActive = () => {
  if (routeNodes.value.length === 0) {
    activeNodeKey.value = ''
    return
  }
  const exists = routeNodes.value.some((n) => n.key === activeNodeKey.value)
  if (!exists) {
    const first = routeNodes.value[0]
    if (first) activeNodeKey.value = first.key
  }
}

const loadRoute = () => {
  try {
    const raw = localStorage.getItem(storageKey.value)
    if (raw) {
      const parsed = JSON.parse(raw) as NodeConfig[]
      routeNodes.value = Array.isArray(parsed) ? parsed.map(cloneNode) : libraryNodes.map(cloneNode)
    } else {
      routeNodes.value = libraryNodes.map(cloneNode)
    }
  } catch {
    routeNodes.value = libraryNodes.map(cloneNode)
  }
  ensureActive()
}

watch(
  () => currentProduct.value,
  () => {
    loadRoute()
  },
  { immediate: true },
)

watch(
  () => activeNode.value,
  (n) => {
    if (!n) return
    draftConfig.key = n.key
    draftConfig.name = n.name
    draftConfig.machineType = n.machineType
    draftConfig.stitchLength = n.stitchLength
    draftConfig.stitchType = n.stitchType
    draftConfig.eta = n.eta
  },
  { immediate: true },
)

const upsertFromLibrary = (key: string) => {
  const lib = libraryNodes.find((n) => n.key === key)
  if (!lib) return
  const existing = routeNodes.value.find((n) => n.key === key)
  if (!existing) routeNodes.value.push(cloneNode(lib))
  activeNodeKey.value = key
}

const applyConfig = () => {
  if (!draftConfig.key) return
  const idx = routeNodes.value.findIndex((n) => n.key === draftConfig.key)
  if (idx < 0) return
  const current = routeNodes.value[idx]
  if (!current) return
  routeNodes.value[idx] = {
    key: draftConfig.key,
    name: current.name,
    machineType: draftConfig.machineType,
    stitchLength: draftConfig.stitchLength,
    stitchType: draftConfig.stitchType,
    eta: draftConfig.eta,
  }
  message.success('已应用节点参数')
}

const saveRoute = () => {
  localStorage.setItem(storageKey.value, JSON.stringify(routeNodes.value))
  message.success('已保存工艺路线')
}

const clearCanvas = () => {
  routeNodes.value = []
  activeNodeKey.value = ''
  message.info('已清空画布')
}
</script>

<template>
  <div class="process-page">
    <div class="page-header">
      <a-typography-title :level="2" style="margin: 0">工艺路线</a-typography-title>
      <div class="right-actions">
        <a-button type="primary" style="background:#17b36b;border-color:#17b36b" @click="saveRoute">保存工艺路线</a-button>
        <a-button type="primary" @click="clearCanvas">清空画布</a-button>
      </div>
    </div>

    <div class="product-row">
      <div class="label">当前款式：</div>
      <a-select v-model:value="currentProduct" :options="products" style="width: 300px" />
    </div>

    <div class="board">
      <div class="node-library">
        <div class="panel-title">工艺节点库</div>
        <a-button
          v-for="item in libraryNodes"
          :key="item.key"
          block
          class="node-btn"
          :type="activeNodeKey === item.key ? 'primary' : 'default'"
          @click="upsertFromLibrary(item.key)"
        >
          {{ item.name }}
        </a-button>
      </div>

      <div class="canvas">
        <div class="panel-title">工艺路线配置画布</div>
        <div class="grid-area">
          <div class="flow-row" v-if="routeNodes.length > 0">
            <div
              v-for="item in routeNodes"
              :key="item.key"
              class="flow-node"
              :class="{ active: activeNodeKey === item.key }"
              @click="activeNodeKey = item.key"
            >
              {{ item.name }}
            </div>
          </div>
          <div v-else class="empty-tip">点击左侧节点库，将工艺节点加入画布</div>
        </div>
      </div>

      <div class="config-panel">
        <div class="panel-title">节点参数配置（{{ activeNode?.name ?? '-' }}）</div>
        <div class="field-label">机台类型</div>
        <a-input v-model:value="draftConfig.machineType" :disabled="!activeNode" />
        <div class="field-label">缝纫长度</div>
        <a-input v-model:value="draftConfig.stitchLength" :disabled="!activeNode" />
        <div class="field-label">线材类型</div>
        <a-input v-model:value="draftConfig.stitchType" :disabled="!activeNode" />
        <div class="field-label">预计时间</div>
        <a-input v-model:value="draftConfig.eta" :disabled="!activeNode" />
        <a-button type="primary" block style="margin-top: 16px" :disabled="!activeNode" @click="applyConfig">
          应用参数
        </a-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.process-page {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.right-actions {
  display: flex;
  gap: 8px;
}

.product-row {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  gap: 8px;
}

.label {
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}

.board {
  display: grid;
  grid-template-columns: 190px 1fr 250px;
  gap: 12px;
}

.node-library,
.canvas,
.config-panel {
  border: 1px solid #e5e7eb;
  background: #ffffff;
  border-radius: 8px;
  padding: 12px;
}

.panel-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 12px;
}

.node-btn {
  margin-bottom: 8px;
  text-align: left;
}

.grid-area {
  height: 420px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background-image: linear-gradient(#eef2f7 1px, transparent 1px), linear-gradient(90deg, #eef2f7 1px, transparent 1px);
  background-size: 24px 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.flow-row {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  justify-content: center;
}

.flow-node {
  min-width: 120px;
  text-align: center;
  padding: 12px 14px;
  border-radius: 6px;
  border: 1px solid #cbd5e1;
  background: #f8fafc;
  color: #0f172a;
  cursor: pointer;
}

.flow-node.active {
  background: #1677ff;
  border-color: #1677ff;
  color: #ffffff;
}

.field-label {
  margin: 10px 0 6px;
  color: #475569;
}

.empty-tip {
  color: #64748b;
  font-weight: 600;
}
</style>
