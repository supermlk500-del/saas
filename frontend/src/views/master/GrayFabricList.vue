<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchGrayFabrics, type GrayFabricItem } from '@/api/master/grayFabric'

type SupplierMetric = {
  supplier: string
  incoming: number
  passRate: number
}

const loading = ref(false)
const tableData = ref<GrayFabricItem[]>([])

const supplierMetrics = ref<SupplierMetric[]>([
  { supplier: '江苏棉纺厂', incoming: 420, passRate: 98.2 },
  { supplier: '浙江化纤公司', incoming: 360, passRate: 96.4 },
  { supplier: '广东纺织厂', incoming: 280, passRate: 94.6 },
  { supplier: '山东混纺厂', incoming: 310, passRate: 97.1 },
])

const compositionData = ref([
  { name: '100% 棉', value: 42, color: '#1677ff' },
  { name: '棉涤混纺', value: 28, color: '#fa8c16' },
  { name: '100% 涤纶', value: 18, color: '#52c41a' },
  { name: '其他', value: 12, color: '#13c2c2' },
])

const kpiData = computed(() => {
  const activeSuppliers = new Set(tableData.value.map((item) => item.supplier)).size
  const materialTypes = new Set(tableData.value.map((item) => item.yarn)).size
  const defects = supplierMetrics.value.reduce((sum, item) => sum + (100 - item.passRate), 0)
  const defectRate = defects / supplierMetrics.value.length
  return {
    activeSuppliers,
    materialTypes,
    defectRate: defectRate.toFixed(2),
  }
})

const donutStyle = computed(() => {
  let current = 0
  const slices: string[] = []
  for (const item of compositionData.value) {
    const next = current + item.value
    slices.push(`${item.color} ${current}% ${next}%`)
    current = next
  }
  return {
    background: `conic-gradient(${slices.join(',')})`,
  }
})

const incomingMax = computed(() => Math.max(...supplierMetrics.value.map((item) => item.incoming), 1))

const columns = [
  { title: '物料编码', dataIndex: 'code', key: 'code', width: 180 },
  { title: '纱线成分', dataIndex: 'yarn', key: 'yarn' },
  { title: '克重', dataIndex: 'weight', key: 'weight', width: 140 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
]

const loadData = async () => {
  loading.value = true
  tableData.value = await fetchGrayFabrics()
  loading.value = false
}

onMounted(loadData)
</script>

<template>
  <div class="material-dashboard">
    <div class="kpi-row">
      <a-card :bordered="false" class="kpi-card">
        <div class="kpi-label">活跃供应商总数</div>
        <div class="kpi-value">{{ kpiData.activeSuppliers }}</div>
        <div class="kpi-foot">稳定供货中</div>
      </a-card>
      <a-card :bordered="false" class="kpi-card">
        <div class="kpi-label">物料类型总数</div>
        <div class="kpi-value">{{ kpiData.materialTypes }}</div>
        <div class="kpi-foot">当前主数据版本</div>
      </a-card>
      <a-card :bordered="false" class="kpi-card alert">
        <div class="kpi-label">来料质检不良率</div>
        <div class="kpi-value">{{ kpiData.defectRate }}%</div>
        <div class="kpi-foot">预警阈值 4.00%</div>
      </a-card>
    </div>

    <div class="chart-row">
      <a-card :bordered="false" class="chart-card">
        <template #title>
          <div class="card-title">纱线成分分布</div>
        </template>
        <div class="donut-wrap">
          <div class="donut" :style="donutStyle">
            <div class="donut-inner">100%</div>
          </div>
          <div class="legend-list">
            <div class="legend-item" v-for="item in compositionData" :key="item.name">
              <span class="dot" :style="{ background: item.color }"></span>
              <span class="legend-name">{{ item.name }}</span>
              <span class="legend-value">{{ item.value }}%</span>
            </div>
          </div>
        </div>
      </a-card>

      <a-card :bordered="false" class="chart-card">
        <template #title>
          <div class="card-title">供应商来料量与质检通过率</div>
        </template>
        <div class="bars-wrap">
          <div class="bars-head">
            <span class="blue">● Incoming</span>
            <span class="green">● Pass Rate</span>
          </div>
          <div class="bar-group" v-for="item in supplierMetrics" :key="item.supplier">
            <div class="supplier">{{ item.supplier }}</div>
            <div class="bar-track">
              <div class="bar incoming" :style="{ width: `${(item.incoming / incomingMax) * 100}%` }"></div>
              <div class="bar pass" :style="{ width: `${item.passRate}%` }"></div>
            </div>
            <div class="bar-val">{{ item.incoming }}t / {{ item.passRate }}%</div>
          </div>
        </div>
      </a-card>
    </div>

    <a-card :bordered="false" class="table-card">
      <template #title>
        <div class="card-title">来料管理主数据</div>
      </template>
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        row-key="key"
        :pagination="{ pageSize: 8, showSizeChanger: false }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === '启用' ? 'success' : 'default'">
              {{ record.status }}
            </a-tag>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<style scoped>
.material-dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: #f5f7fb;
  min-height: calc(100vh - 130px);
}

.kpi-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.kpi-card {
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.05);
}

.kpi-label {
  font-size: 13px;
  color: #64748b;
}

.kpi-value {
  margin-top: 10px;
  font-size: 34px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1;
}

.kpi-foot {
  margin-top: 10px;
  color: #94a3b8;
  font-size: 12px;
}

.kpi-card.alert {
  background: linear-gradient(135deg, #fff1f0 0%, #fff7e8 100%);
}

.kpi-card.alert .kpi-value {
  color: #cf1322;
}

.chart-row {
  display: grid;
  grid-template-columns: 1.1fr 1.4fr;
  gap: 16px;
}

.chart-card,
.table-card {
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.05);
}

.card-title {
  font-weight: 700;
  color: #0f172a;
}

.donut-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  min-height: 240px;
}

.donut {
  width: 210px;
  height: 210px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: inset 0 0 0 1px rgba(15, 23, 42, 0.06);
}

.donut-inner {
  width: 118px;
  height: 118px;
  border-radius: 50%;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #1d4ed8;
  font-size: 24px;
  font-weight: 700;
  box-shadow: 0 0 0 1px #e2e8f0;
}

.legend-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.legend-name {
  color: #334155;
}

.legend-value {
  margin-left: auto;
  font-weight: 600;
  color: #0f172a;
}

.bars-wrap {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.bars-head {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #64748b;
}

.blue {
  color: #1677ff;
}

.green {
  color: #52c41a;
}

.bar-group {
  display: grid;
  grid-template-columns: 112px 1fr 120px;
  align-items: center;
  gap: 12px;
}

.supplier {
  font-size: 12px;
  color: #334155;
}

.bar-track {
  position: relative;
  height: 30px;
  border-radius: 8px;
  background: #f8fafc;
  overflow: hidden;
  box-shadow: inset 0 0 0 1px #e2e8f0;
}

.bar {
  position: absolute;
  left: 0;
  border-radius: 8px;
}

.bar.incoming {
  top: 5px;
  height: 8px;
  background: linear-gradient(90deg, #1677ff 0%, #69b1ff 100%);
}

.bar.pass {
  bottom: 5px;
  height: 8px;
  background: linear-gradient(90deg, #52c41a 0%, #95de64 100%);
}

.bar-val {
  text-align: right;
  font-size: 12px;
  color: #475569;
}

:deep(.ant-table-thead > tr > th) {
  background: #f8fafc;
  color: #334155;
  font-weight: 600;
}

@media (max-width: 1280px) {
  .kpi-row,
  .chart-row {
    grid-template-columns: 1fr;
  }

  .donut-wrap {
    flex-direction: column;
    align-items: flex-start;
  }

  .bar-group {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .bar-val {
    text-align: left;
  }
}
</style>
