import { defineStore } from 'pinia'

export type SchedulePriority = 'High' | 'Normal' | 'Low'

export type PlanRecord = {
  key: string
  planNo: string
  orderNo: string
  line: string
  status: string
  startDate?: string
}

export type PendingOrder = {
  id: string
  fabric: string
  qty: string
  priority: SchedulePriority
  planNo?: string
}

const STORAGE_KEY = 'schedule_state_v1'

const defaultPlans: PlanRecord[] = [
  { key: '1', planNo: 'PLAN-2026001', orderNo: 'ORD-102', line: '产线1', status: '已生成', startDate: '2026-04-07' },
  { key: '2', planNo: 'PLAN-2026002', orderNo: 'ORD-103', line: '产线2', status: '排产中', startDate: '2026-04-08' },
]

const defaultPendingOrders: PendingOrder[] = [
  { id: 'ORD-102', fabric: '全棉 32S 平纹', qty: '5000m', priority: 'High', planNo: 'PLAN-2026001' },
  { id: 'ORD-103', fabric: '斜纹色丁', qty: '8000m', priority: 'Normal', planNo: 'PLAN-2026002' },
  { id: 'ORD-105', fabric: '棉涤混纺', qty: '6000m', priority: 'High' },
  { id: 'ORD-106', fabric: '全涤缎纹', qty: '4500m', priority: 'Normal' },
]

const formatPlanNo = () => {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  const t = String(now.getTime()).slice(-4)
  return `PLAN-${y}${m}${d}-${t}`
}

export const useScheduleStore = defineStore('schedule', {
  state: () => ({
    hydrated: false,
    plans: [...defaultPlans] as PlanRecord[],
    pendingOrders: [...defaultPendingOrders] as PendingOrder[],
  }),
  actions: {
    hydrate() {
      if (this.hydrated) return
      const raw = localStorage.getItem(STORAGE_KEY)
      if (raw) {
        try {
          const parsed = JSON.parse(raw) as { plans?: PlanRecord[]; pendingOrders?: PendingOrder[] }
          if (Array.isArray(parsed.plans)) this.plans = parsed.plans
          if (Array.isArray(parsed.pendingOrders)) this.pendingOrders = parsed.pendingOrders
        } catch {
          this.plans = [...defaultPlans]
          this.pendingOrders = [...defaultPendingOrders]
        }
      }
      this.hydrated = true
    },
    persist() {
      localStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({ plans: this.plans, pendingOrders: this.pendingOrders }),
      )
    },
    generatePlan(payload: {
      orderNo: string
      line: string
      startDate: string
      fabric: string
      qty: string
      priority: SchedulePriority
    }) {
      const planNo = formatPlanNo()
      const plan: PlanRecord = {
        key: String(Date.now()),
        planNo,
        orderNo: payload.orderNo,
        line: payload.line,
        status: '待排产',
        startDate: payload.startDate,
      }

      this.plans.unshift(plan)

      const exists = this.pendingOrders.some((item) => item.id === payload.orderNo)
      if (!exists) {
        this.pendingOrders.unshift({
          id: payload.orderNo,
          fabric: payload.fabric,
          qty: payload.qty,
          priority: payload.priority,
          planNo,
        })
      }

      this.persist()
    },
    removePendingOrders(orderIds: string[]) {
      const set = new Set(orderIds)
      this.pendingOrders = this.pendingOrders.filter((item) => !set.has(item.id))
      this.persist()
    },
    markPlansScheduling(orderIds: string[]) {
      const set = new Set(orderIds)
      this.plans = this.plans.map((plan) =>
        set.has(plan.orderNo) ? { ...plan, status: '排产中' } : plan,
      )
      this.persist()
    },
  },
})
