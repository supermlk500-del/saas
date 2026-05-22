<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  RocketOutlined,
  CalendarOutlined,
  ClockCircleOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons-vue'

type Priority = 'High' | 'Normal' | 'Low'
type Order = {
  id: string
  fabric: string
  qty: string
  priority: Priority
}

type Machine = {
  id: string
  name: string
  workshop: string
}

type TaskStatus = 'Scheduled' | 'Completed'
type Task = {
  id: string
  machineId: string
  orderId: string
  status: TaskStatus
  progress: number
  startDay: number // 0-6 (index in timelineDays)
  startShift: number // 0-2 (早中晚)
  durationShifts: number // 持续班次数
  estFinish: string
  qcAlert?: boolean
}

const viewMode = ref<'Day' | 'Week' | 'Month'>('Day')

const pendingOrders = ref<Order[]>([
  { id: 'ORD-101', fabric: '全棉 32S 平纹', qty: '5000m', priority: 'High' },
  { id: 'ORD-104', fabric: '斜纹色丁', qty: '8000m', priority: 'Low' },
])

const machines = ref<Machine[]>([
  { id: 'L-01', name: 'Loom-01', workshop: '一号织造车间' },
  { id: 'L-02', name: 'Loom-02', workshop: '一号织造车间' },
  { id: 'L-03', name: 'Loom-03', workshop: '一号织造车间' },
  { id: 'S-01', name: 'Sizing-01', workshop: '二号准备车间' },
  { id: 'W-01', name: 'Warping-01', workshop: '二号准备车间' },
])

const scheduledTasks = ref<Task[]>([
  {
    id: 'T-1',
    machineId: 'L-01',
    orderId: 'ORD-102',
    status: 'Scheduled',
    progress: 45,
    startDay: 0,
    startShift: 1,
    durationShifts: 5,
    estFinish: '4/09',
    qcAlert: true,
  },
  {
    id: 'T-2',
    machineId: 'L-02',
    orderId: 'ORD-103',
    status: 'Completed',
    progress: 100,
    startDay: 0,
    startShift: 0,
    durationShifts: 3,
    estFinish: '4/08',
  },
  {
    id: 'T-3',
    machineId: 'S-01',
    orderId: 'ORD-105',
    status: 'Scheduled',
    progress: 12,
    startDay: 2,
    startShift: 0,
    durationShifts: 4,
    estFinish: '4/10',
  },
  {
    id: 'T-4',
    machineId: 'L-03',
    orderId: 'ORD-106',
    status: 'Scheduled',
    progress: 0,
    startDay: 3,
    startShift: 1,
    durationShifts: 6,
    estFinish: '4/12',
  },
])

const timelineDays = ['4/07', '4/08', '4/09', '4/10', '4/11', '4/12', '4/13']
const shifts = ['Morning', 'Afternoon', 'Evening']

const getPriorityColor = (p: Priority) => {
  switch (p) {
    case 'High': return '#ff4d4f'
    case 'Normal': return '#1890ff'
    case 'Low': return '#d9d9d9'
  }
}

const getTaskStyle = (task: Task) => {
  const cellWidth = 40 // matched with .grid-cell min-width
  const startOffset = (task.startDay * 3 + task.startShift) * cellWidth
  const width = task.durationShifts * cellWidth
  
  let backgroundColor = task.status === 'Completed' ? '#52c41a' : '#1890ff'
  let boxShadow = 'none'
  let animation = 'none'
  let zIndex = 2

  if (task.qcAlert) {
    backgroundColor = '#ff4d4f'
    boxShadow = '0 0 20px rgba(255, 77, 79, 0.8)'
    animation = 'pulse-red 2s infinite'
    zIndex = 10
  }
  
  return {
    left: `${startOffset}px`,
    width: `${width}px`,
    backgroundColor,
    boxShadow,
    animation,
    zIndex
  }
}

const getTasksByMachine = (machineId: string) => {
  return scheduledTasks.value.filter(t => t.machineId === machineId)
}

// Drawer related
const drawerVisible = ref(false)
const selectedTask = ref<Task | null>(null)

const handleTaskClick = (task: Task) => {
  if (task.qcAlert) {
    selectedTask.value = task
    drawerVisible.value = true
  }
}
</script>

<template>
  <div class="schedule-workbench">
    <!-- Top Header -->
    <div class="workbench-header">
      <div class="header-left">
        <h2 class="page-title">生产排产工作台</h2>
        <a-radio-group v-model:value="viewMode" size="small" class="view-switcher">
          <a-radio-button value="Day">日视图</a-radio-button>
          <a-radio-button value="Week">周视图</a-radio-button>
          <a-radio-button value="Month">月视图</a-radio-button>
        </a-radio-group>
      </div>
      <div class="header-right">
        <a-space>
          <a-button class="auto-schedule-btn" type="primary">
            <template #icon><ThunderboltOutlined /></template>
            自动排产
          </a-button>
        </a-space>
      </div>
    </div>

    <div class="main-layout">
      <!-- Left: Pending Orders Pool -->
      <div class="orders-pool">
        <div class="pool-title">
          待排订单池
          <a-badge :count="pendingOrders.length" :number-style="{ backgroundColor: '#ff7a45' }" />
        </div>
        <div class="pool-content">
          <a-card v-for="order in pendingOrders" :key="order.id" class="order-card" size="small">
            <div class="order-card-header">
              <span class="order-id">{{ order.id }}</span>
              <a-tag :color="getPriorityColor(order.priority)" class="priority-tag">{{ order.priority }}</a-tag>
            </div>
            <div class="order-fabric">{{ order.fabric }}</div>
            <div class="order-footer">
              <span class="order-qty">数量: {{ order.qty }}</span>
            </div>
          </a-card>
        </div>
      </div>

      <!-- Center: Gantt Chart Area -->
      <div class="gantt-container">
        <!-- Gantt Header (Timeline) -->
        <div class="gantt-header-row">
          <div class="resource-col-header">设备 / 车间</div>
          <div class="timeline-header">
            <div v-for="day in timelineDays" :key="day" class="day-col">
              <div class="day-label">{{ day }}</div>
              <div class="shift-labels">
                <span v-for="shift in shifts" :key="shift">{{ shift }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Gantt Body -->
        <div class="gantt-body">
          <div v-for="machine in machines" :key="machine.id" class="gantt-row">
            <div class="resource-info">
              <div class="machine-name">{{ machine.name }}</div>
              <div class="workshop-name">{{ machine.workshop }}</div>
            </div>
            <div class="grid-cells">
              <div v-for="n in timelineDays.length * 3" :key="n" class="grid-cell"></div>
              
              <!-- Task Blocks -->
              <template v-for="task in getTasksByMachine(machine.id)" :key="task.id">
                <a-tooltip placement="top">
                  <template #title>
                    <div class="gantt-tooltip">
                      <div class="tooltip-id">{{ task.orderId }}</div>
                      <div class="tooltip-progress">当前进度: {{ task.progress }}%</div>
                      <div class="tooltip-finish">预计完成: {{ task.estFinish }}</div>
                    </div>
                  </template>
                  <div
                    class="task-block"
                    :style="getTaskStyle(task)"
                    @click="handleTaskClick(task)"
                  >
                    <span class="task-label">{{ task.orderId }}</span>
                  </div>
                </a-tooltip>
              </template>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- QC Anomaly Drawer -->
    <a-drawer
      v-model:open="drawerVisible"
      title="质检异常联动 (Loom-01)"
      placement="right"
      width="460"
      :header-style="{ borderBottom: '1px solid #f1f5f9' }"
      :body-style="{ padding: '24px' }"
    >
      <template #title>
        <span style="color: #ff4d4f; font-weight: 700; font-size: 18px;">QC Anomaly Linkage (Loom-01)</span>
      </template>
      
      <div class="drawer-content">
        <div class="anomaly-summary">
          <a-image
            :width="120"
            class="defect-thumb"
            src="https://picsum.photos/seed/oilstain/200/200"
            fallback="https://via.placeholder.com/120?text=Defect+Image"
          />
          <div class="summary-text">
            <div class="anomaly-label">实时异常监控</div>
            <div class="anomaly-time">检测时间: 10:45:22 AM</div>
          </div>
        </div>

        <a-descriptions title="Production Impact" :column="1" bordered size="small" class="custom-descriptions">
          <a-descriptions-item label="Original Order">
            <span class="order-link">{{ selectedTask?.orderId }} (Downgraded)</span>
          </a-descriptions-item>
          <a-descriptions-item label="Defect Type">
            <a-tag color="error">油污 (Oil Stain)</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="Quantity Lost">
            <span style="color: #ff4d4f; font-weight: 800; font-size: 16px;">300m</span>
          </a-descriptions-item>
          <a-descriptions-item label="Operator">张伟 (Shift-A)</a-descriptions-item>
        </a-descriptions>

        <div class="drawer-actions">
          <div class="action-title">决策处理建议</div>
          <a-space direction="vertical" style="width: 100%" size="middle">
            <a-button type="primary" block size="large" style="background: #ff7a45; border-color: #ff7a45">
              自动补单 (Auto-Replenish Order)
            </a-button>
            <a-button type="primary" block size="large">
              重新排产 (Reschedule)
            </a-button>
            <a-button block size="large">
              忽略 (Ignore)
            </a-button>
          </a-space>
        </div>
      </div>
    </a-drawer>
  </div>
</template>

<style scoped>
.schedule-workbench {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px);
  background: #f8fafc;
  gap: 16px;
}

.workbench-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 4px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
}

.auto-schedule-btn {
  background-color: #ff7a45;
  border-color: #ff7a45;
  height: 36px;
  border-radius: 8px;
  font-weight: 600;
}

.auto-schedule-btn:hover {
  background-color: #ff9c6e;
  border-color: #ff9c6e;
}

.main-layout {
  display: flex;
  flex: 1;
  gap: 16px;
  min-height: 0;
}

/* Left Sidebar */
.orders-pool {
  width: 260px;
  background: #ffffff;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
}

.pool-title {
  padding: 16px;
  font-weight: 700;
  font-size: 15px;
  color: #1e293b;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pool-content {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-card {
  border-radius: 12px;
  border: 1px solid #f1f5f9;
  cursor: grab;
  transition: all 0.2s;
}

.order-card:hover {
  border-color: #ff7a45;
  box-shadow: 0 4px 12px rgba(255, 122, 69, 0.1);
}

.order-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.order-id {
  font-weight: 700;
  color: #475569;
}

.priority-tag {
  font-size: 10px;
  margin: 0;
  padding: 0 4px;
}

.order-fabric {
  font-size: 13px;
  color: #1e293b;
  font-weight: 500;
  margin-bottom: 8px;
}

.order-footer {
  font-size: 12px;
  color: #64748b;
}

/* Center Gantt Area */
.gantt-container {
  flex: 1;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.gantt-header-row {
  display: flex;
  border-bottom: 1px solid #f1f5f9;
  background: #fcfcfd;
}

.resource-col-header {
  width: 180px;
  padding: 12px 16px;
  font-weight: 600;
  color: #64748b;
  border-right: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
}

.timeline-header {
  flex: 1;
  display: flex;
  overflow-x: auto;
}

.day-col {
  flex: 1;
  min-width: 120px;
  border-right: 1px solid #f1f5f9;
  display: flex;
  flex-direction: column;
}

.day-label {
  padding: 8px;
  text-align: center;
  font-weight: 700;
  font-size: 13px;
  color: #1e293b;
  border-bottom: 1px solid #f8fafc;
}

.shift-labels {
  display: flex;
  justify-content: space-around;
  font-size: 11px;
  color: #94a3b8;
  padding: 4px 0;
}

.gantt-body {
  flex: 1;
  overflow-y: auto;
}

.gantt-row {
  display: flex;
  height: 64px;
  border-bottom: 1px solid #f1f5f9;
}

.resource-info {
  width: 180px;
  padding: 0 16px;
  border-right: 1px solid #f1f5f9;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: #ffffff;
}

.machine-name {
  font-weight: 700;
  color: #1e293b;
  font-size: 14px;
}

.workshop-name {
  font-size: 11px;
  color: #94a3b8;
}

.grid-cells {
  flex: 1;
  display: flex;
  position: relative;
  height: 64px;
}

.grid-cell {
  flex: 1;
  min-width: 40px;
  border-right: 1px solid #f1f5f9;
  background-image: 
    linear-gradient(to right, #f1f5f9 1px, transparent 1px);
  background-size: 33.33% 100%;
}

.task-block {
  position: absolute;
  top: 0;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-weight: 700;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: none;
}

.task-block:hover {
  filter: brightness(1.1);
  box-shadow: inset 0 0 0 2px rgba(255, 255, 255, 0.3);
}

.task-label {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-align: center;
  width: 100%;
}

/* Tooltip Styles */
.gantt-tooltip {
  padding: 4px;
}

.tooltip-id {
  font-weight: 800;
  font-size: 14px;
  margin-bottom: 4px;
  color: #ffffff;
}

.tooltip-progress,
.tooltip-finish {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.85);
}

/* QC Alert Pulsing Animation */
@keyframes pulse-red {
  0% {
    box-shadow: 0 0 0 0 rgba(255, 77, 79, 0.7);
    filter: brightness(1);
  }
  70% {
    box-shadow: 0 0 0 15px rgba(255, 77, 79, 0);
    filter: brightness(1.2);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(255, 77, 79, 0);
    filter: brightness(1);
  }
}

/* Drawer Styles */
.drawer-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.anomaly-summary {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  padding: 16px;
  background: #fff1f0;
  border-radius: 12px;
  border: 1px solid #ffa39e;
}

.defect-thumb {
  border-radius: 8px;
  border: 2px solid #ffffff;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.anomaly-label {
  font-weight: 700;
  color: #cf1322;
  font-size: 15px;
  margin-bottom: 4px;
}

.anomaly-time {
  font-size: 12px;
  color: #8c8c8c;
}

.order-link {
  color: #1890ff;
  font-weight: 600;
  text-decoration: underline;
}

.custom-descriptions :deep(.ant-descriptions-title) {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 12px;
}

.drawer-actions {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 8px;
}

.action-title {
  font-weight: 700;
  color: #1e293b;
  font-size: 14px;
}
</style>
