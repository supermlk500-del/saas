<script setup lang="ts">
import { computed } from 'vue'
import {
  InboxOutlined,
  ExclamationCircleOutlined,
  CheckCircleOutlined,
  BarChartOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
} from '@ant-design/icons-vue'

type MetricCard = {
  title: string
  value: string
  trend?: string
  trendType?: 'up' | 'down'
  icon?: any
  iconBg?: string
  iconColor?: string
}

type TrendPoint = {
  month: string
  value: number
}

type EfficiencyBar = {
  name: string
  value: number
}

type OrderStatus = {
  id: string
  progress: number
  status: string
}

type PieSegment = {
  label: string
  value: number
  color: string
}

const metricCards: MetricCard[] = [
  {
    title: '今日待检批次',
    value: '12',
    trend: '15%',
    trendType: 'down',
    icon: InboxOutlined,
    iconBg: '#fff7ed',
    iconColor: '#ff7a45',
  },
  {
    title: '异常拒收数',
    value: '3',
    trend: '2',
    trendType: 'down',
    icon: ExclamationCircleOutlined,
    iconBg: '#fef2f2',
    iconColor: '#ef4444',
  },
  {
    title: '本周放行率',
    value: '94.2%',
    trend: '1.2%',
    trendType: 'up',
    icon: CheckCircleOutlined,
    iconBg: '#f0fdf4',
    iconColor: '#22c55e',
  },
  {
    title: '排产池负荷',
    value: '45k',
    trend: '米',
    trendType: 'up',
    icon: BarChartOutlined,
    iconBg: '#fff7ed',
    iconColor: '#ff7a45',
  },
]

const trendData: TrendPoint[] = [
  { month: '5月', value: 420 },
  { month: '6月', value: 468 },
  { month: '7月', value: 512 },
  { month: '8月', value: 576 },
  { month: '9月', value: 618 },
  { month: '10月', value: 655 },
  { month: '11月', value: 702 },
  { month: '12月', value: 748 },
  { month: '1月', value: 786 },
  { month: '2月', value: 842 },
  { month: '3月', value: 905 },
  { month: '4月', value: 968 },
]

const efficiencyBars: EfficiencyBar[] = [
  { name: '生产线A', value: 78 },
  { name: '生产线B', value: 91 },
  { name: '生产线C', value: 66 },
  { name: '生产线D', value: 84 },
  { name: '生产线E', value: 73 },
  { name: '生产线F', value: 88 },
  { name: '生产线G', value: 80 },
  { name: '生产线H', value: 94 },
]

const orderStatuses: OrderStatus[] = [
  { id: '20260407001', progress: 88, status: '生产中' },
  { id: '20260407002', progress: 64, status: '已排产' },
  { id: '20260407003', progress: 79, status: '待质检' },
  { id: '20260407004', progress: 53, status: '裁剪完成' },
  { id: '20260407005', progress: 21, status: '待投产' },
]

const pieSegments: PieSegment[] = [
  { label: '时间节省', value: 28.4, color: '#ff7a45' },
  { label: '能耗降低', value: 21.6, color: '#ff9c6e' },
  { label: '换线效率提升', value: 17.3, color: '#ffbb96' },
  { label: '设备利用提升', value: 15.2, color: '#ffd8bf' },
  { label: '延期风险降低', value: 7.5, color: '#ffe7ba' },
  { label: '人工干预减少', value: 10, color: '#fff1e6' },
]

const trendPoints = computed(() => {
  const width = 520
  const height = 220
  const max = Math.max(...trendData.map((item) => item.value))
  return trendData
    .map((item, index) => {
      const x = (index / (trendData.length - 1)) * width
      const y = height - (item.value / max) * 180 - 20
      return `${x},${y}`
    })
    .join(' ')
})

const trendFillPoints = computed(() => `0,220 ${trendPoints.value} 520,220`)

const pieGradient = computed(() => {
  let current = 0
  const parts = pieSegments.map((item) => {
    const start = current
    current += item.value
    return `${item.color} ${start}% ${current}%`
  })
  return `conic-gradient(${parts.join(', ')})`
})
</script>

<template>
  <div class="dashboard-page">
    <div class="metric-grid">
      <a-card v-for="item in metricCards" :key="item.title" class="metric-card" :bordered="false">
        <div class="metric-header">
          <div class="metric-icon" :style="{ background: item.iconBg, color: item.iconColor }">
            <component :is="item.icon" />
          </div>
          <div v-if="item.trend" class="metric-trend" :class="item.trendType">
            <component :is="item.trendType === 'up' ? ArrowUpOutlined : ArrowDownOutlined" class="trend-arrow" />
            {{ item.trend }}
          </div>
        </div>
        <div class="metric-info">
          <div class="metric-title">{{ item.title }}</div>
          <div class="metric-value">{{ item.value }}</div>
        </div>
      </a-card>
    </div>

    <div class="panel-grid">
      <a-card class="panel-card chart-card" :bordered="false">
        <template #title>
          <div class="panel-title">
            <div class="title-dot"></div>
            实时质检动态
          </div>
        </template>
        <template #extra><a href="#" class="extra-link">查看全部</a></template>
        <div class="quality-list">
          <div v-for="i in 2" :key="i" class="quality-item">
            <div class="item-left">
              <div class="status-dot" :class="i === 1 ? 'pending' : 'inspecting'"></div>
              <div class="item-info">
                <div class="item-code">TSK-2405-00{{ i }}</div>
                <div class="item-sub">批次: BAT-20240520-0{{ i }}</div>
              </div>
            </div>
            <div class="item-right">
              <div class="item-time">10:30 AM</div>
              <a-tag :color="i === 1 ? 'orange' : 'blue'">{{ i === 1 ? '待检验' : '检验中' }}</a-tag>
            </div>
          </div>
        </div>
      </a-card>

      <a-card class="panel-card chart-card" :bordered="false">
        <template #title>
          <div class="panel-title">
            <div class="title-dot"></div>
            即将排产 (高优先级)
          </div>
        </template>
        <template #extra><a href="#" class="extra-link">进入排产板</a></template>
        <div class="schedule-list">
          <div v-for="i in 2" :key="i" class="schedule-item">
            <div class="item-left">
              <div class="item-info">
                <div class="item-code-row">
                  <span class="item-code">BAT-20240518-1{{ i }}</span>
                  <a-tag size="small" :color="i === 1 ? 'orange' : 'gold'">{{ i === 1 ? 'A级' : 'B级' }}</a-tag>
                </div>
                <div class="item-sub">{{ i === 1 ? '纯棉汗布' : '涤纶网眼' }}</div>
              </div>
            </div>
            <div class="item-right">
              <div class="item-qty">2,500m</div>
              <div class="item-sub">可用余量</div>
            </div>
          </div>
        </div>
      </a-card>
    </div>

    <div class="panel-grid">
      <a-card title="月度产量趋势" class="panel-card" :bordered="false">
        <div class="line-chart">
          <div class="y-axis">
            <span>1000</span>
            <span>800</span>
            <span>600</span>
            <span>400</span>
            <span>200</span>
            <span>0</span>
          </div>
          <div class="chart-content">
            <div class="grid-lines">
              <span v-for="i in 6" :key="i"></span>
            </div>
            <svg viewBox="0 0 520 220" class="chart-svg" preserveAspectRatio="none">
              <polygon :points="trendFillPoints" fill="rgba(255, 122, 69, 0.08)" />
              <polyline :points="trendPoints" fill="none" stroke="#ff7a45" stroke-width="3" stroke-linecap="round" />
            </svg>
            <div class="x-axis">
              <span v-for="item in trendData" :key="item.month">{{ item.month }}</span>
            </div>
          </div>
        </div>
      </a-card>

      <a-card title="AI 调度优化成效" class="panel-card" :bordered="false">
        <div class="pie-layout">
          <div class="pie-chart" :style="{ backgroundImage: pieGradient }">
            <div class="pie-hole"></div>
          </div>
          <div class="pie-legend">
            <div v-for="item in pieSegments" :key="`${item.label}-${item.value}`" class="legend-row">
              <span class="legend-dot" :style="{ background: item.color }"></span>
              <span class="legend-label">{{ item.label }}</span>
              <span class="legend-value">{{ item.value }}%</span>
            </div>
          </div>
        </div>
      </a-card>
    </div>
  </div>
</template>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
}

.metric-card {
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
  transition: all 0.3s ease;
}

.metric-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06);
}

.metric-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.metric-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.metric-trend {
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border-radius: 99px;
}

.metric-trend.up {
  color: #22c55e;
  background: #f0fdf4;
}

.metric-trend.down {
  color: #ef4444;
  background: #fef2f2;
}

.trend-arrow {
  font-size: 12px;
}

.metric-title {
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 4px;
}

.metric-value {
  color: #1e293b;
  font-size: 32px;
  font-weight: 700;
  line-height: 1.2;
}

.panel-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.panel-card {
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
}

.title-dot {
  width: 4px;
  height: 16px;
  background: #ff7a45;
  border-radius: 2px;
}

.extra-link {
  color: #ff7a45;
  font-size: 13px;
  font-weight: 500;
}

.quality-list,
.schedule-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.quality-item,
.schedule-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #f8fafc;
  border-radius: 12px;
  border: 1px solid #f1f5f9;
}

.item-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot.pending {
  background: #ff7a45;
  box-shadow: 0 0 0 4px rgba(255, 122, 69, 0.1);
}

.status-dot.inspecting {
  background: #3b82f6;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.1);
}

.item-code {
  font-weight: 700;
  color: #1e293b;
  font-size: 14px;
}

.item-code-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.item-sub {
  color: #64748b;
  font-size: 12px;
  margin-top: 2px;
}

.item-right {
  text-align: right;
}

.item-time {
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 4px;
}

.item-qty {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
}

.line-chart {
  display: flex;
  gap: 12px;
  height: 250px;
  margin-top: 12px;
}

.y-axis {
  width: 40px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  color: #94a3b8;
  font-size: 11px;
}

.chart-content {
  position: relative;
  flex: 1;
  height: 100%;
}

.grid-lines {
  position: absolute;
  inset: 0 0 26px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.grid-lines span {
  border-top: 1px dashed #e2e8f0;
}

.chart-svg {
  position: absolute;
  inset: 0 0 26px;
  width: 100%;
  height: calc(100% - 26px);
}

.x-axis {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  color: #94a3b8;
  font-size: 11px;
  text-align: center;
}

.pie-layout {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 40px;
  padding: 10px;
}

.pie-chart {
  width: 180px;
  height: 180px;
  border-radius: 50%;
  position: relative;
  flex-shrink: 0;
}

.pie-hole {
  position: absolute;
  inset: 36px;
  border-radius: 50%;
  background: #ffffff;
}

.pie-legend {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.legend-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.legend-label {
  flex: 1;
  color: #64748b;
  font-size: 13px;
}

.legend-value {
  color: #1e293b;
  font-weight: 700;
  font-size: 14px;
}

@media (max-width: 1200px) {
  .metric-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 800px) {
  .metric-grid,
  .panel-grid {
    grid-template-columns: 1fr;
  }
  .pie-layout {
    flex-direction: column;
  }
}
</style>
