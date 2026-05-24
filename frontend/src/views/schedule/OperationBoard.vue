<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchPlans } from '@/api/plan/plan'
import { planStatusOptions } from '@/constants/dictionaries'
import type { ProductionPlanItem } from '@/types/domain'

const loading = ref(false)
const plans = ref<ProductionPlanItem[]>([])

const getStatusMeta = (status?: string) =>
  planStatusOptions.find((item) => item.value === status)

const boardCards = computed(() =>
  plans.value
    .filter((item) => !['COMPLETED', 'CANCELLED'].includes(item.status))
    .slice(0, 6),
)

const loadData = async () => {
  loading.value = true
  try {
    const response = await fetchPlans({
      pageNum: 1,
      pageSize: 20,
    })
    plans.value = response.list
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadData()
})
</script>

<template>
  <a-card class="page-card" :bordered="false">
    <a-spin :spinning="loading">
      <a-space direction="vertical" size="large" style="width: 100%">
        <a-typography-title :level="2" style="margin: 0">工序排程看板</a-typography-title>
        <a-row :gutter="[16, 16]">
          <a-col :span="8" v-for="item in boardCards" :key="item.planId">
            <a-card :title="item.routeName || `路线 ${item.routeId}`" bordered>
              <a-space direction="vertical">
                <div>计划号：#{{ item.planId }}</div>
                <div>批次号：{{ item.batchNo || item.batchId }}</div>
                <div>开始时间：{{ item.planStartTime }}</div>
                <div>结束时间：{{ item.planEndTime || '-' }}</div>
                <a-tag :color="getStatusMeta(item.status)?.color">
                  {{ getStatusMeta(item.status)?.label || item.status }}
                </a-tag>
              </a-space>
            </a-card>
          </a-col>
        </a-row>
        <a-empty v-if="!boardCards.length" description="暂无可展示的排程计划" />
      </a-space>
    </a-spin>
  </a-card>
</template>
