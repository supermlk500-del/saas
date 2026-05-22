import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/dashboard' },

    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('@/views/dashboard/KanbanBoard.vue'),
      meta: { section: '看板', title: '任务看板' },
    },

    {
      path: '/gray/fabric',
      name: 'gray-fabric',
      component: () => import('@/views/master/GrayFabricList.vue'),
      meta: { section: '来料管理', title: '来料主数据' },
    },
    {
      path: '/gray/inbound',
      name: 'gray-inbound',
      component: () => import('@/views/gray/InboundBatchList.vue'),
      meta: { section: '来料管理', title: '来料批次' },
    },
    {
      path: '/gray/iqc-task',
      name: 'gray-iqc-task',
      component: () => import('@/views/quality/TaskList.vue'),
      meta: { section: '来料管理', title: 'IQC任务' },
    },
    {
      path: '/gray/iqc-result',
      name: 'gray-iqc-result',
      component: () => import('@/views/quality/ResultList.vue'),
      meta: { section: '来料管理', title: 'IQC结果' },
    },
    {
      path: '/gray/release',
      name: 'gray-release',
      component: () => import('@/views/gray/ReleaseDecisionList.vue'),
      meta: { section: '来料管理', title: '放行决策' },
    },

    {
      path: '/order/list',
      name: 'order-list',
      component: () => import('@/views/order/OrderList.vue'),
      meta: { section: '订单', title: '订单管理' },
    },

    {
      path: '/process-center/process',
      name: 'process-center-process',
      component: () => import('@/views/master/ProcessList.vue'),
      meta: { section: '工艺中心', title: '工艺路线' },
    },
    {
      path: '/process-center/shift',
      name: 'process-center-shift',
      component: () => import('@/views/master/ShiftList.vue'),
      meta: { section: '工艺中心', title: '班次日历' },
    },
    {
      path: '/master/machine',
      name: 'master-machine',
      component: () => import('@/views/master/MachineList.vue'),
      meta: { section: '设备', title: '机器管理' },
    },
    {
      path: '/master/fabric-process',
      name: 'master-fabric-process',
      component: () => import('@/views/master/FabricProcessList.vue'),
      meta: { section: '工艺中心', title: '工艺档案中心' },
    },
    {
      path: '/master/fabric-defect',
      name: 'master-fabric-defect',
      component: () => import('@/views/master/FabricDefectList.vue'),
      meta: { section: '工艺中心', title: '布料瑕疵种类' },
    },

    {
      path: '/schedule/pool',
      name: 'schedule-pool',
      component: () => import('@/views/schedule/SchedulePool.vue'),
      meta: { section: '排产', title: '排产池' },
    },
    {
      path: '/schedule/main',
      name: 'schedule-main',
      component: () => import('@/views/plan/PlanMain.vue'),
      meta: { section: '排产', title: '主生产计划' },
    },
    {
      path: '/schedule/board',
      name: 'schedule-board',
      component: () => import('@/views/schedule/ProductionGantt.vue'),
      meta: { section: '排产', title: '生产调度甘特图' },
    },
    {
      path: '/schedule/reschedule-log',
      name: 'schedule-reschedule-log',
      component: () => import('@/views/plan/PlanLog.vue'),
      meta: { section: '排产', title: '重排日志' },
    },

    {
      path: '/quality/ncr',
      name: 'quality-ncr',
      component: () => import('@/views/quality/NcrList.vue'),
      meta: { section: '质检', title: 'NCR管理' },
    },
    {
      path: '/quality/realtime',
      name: 'quality-realtime',
      component: () => import('@/views/quality/RealtimeInspect.vue'),
      meta: { section: '质检', title: '实时质检' },
    },
    {
      path: '/quality/rework',
      name: 'quality-rework',
      component: () => import('@/views/quality/ReworkList.vue'),
      meta: { section: '质检', title: '返工单' },
    },

    {
      path: '/:pathMatch(.*)*',
      redirect: '/dashboard',
    },
  ],
})

export default router
