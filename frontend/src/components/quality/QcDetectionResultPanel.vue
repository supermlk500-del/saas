<script setup lang="ts">
import { computed } from 'vue'
import type { InspectionDataItem, PlanStepItem, QcDetectionBox, QcDetectionResult, QcRecordItem } from '@/types/domain'
import type { ResultJudge } from '@/types/dictionary'
import { formatDateTime } from '@/utils/date'
import { isLikelyResultImage, isLikelySourceImage, normalizeImagePath } from '@/utils/image'

const props = defineProps<{
  detailLoading: boolean
  activeRecord: QcRecordItem | null
  detectionResult: QcDetectionResult | null
  activePlanStep?: PlanStepItem
  currentJudge?: string
  currentConfidence?: number | null
  currentDefectType?: string | null
  currentResultValue?: string | null
  sourcePreviewUrl: string
  resultPreviewUrl: string
  sourceImagePath: string
  resultImagePath: string
  currentBoxes: QcDetectionBox[]
  activeAttachments: InspectionDataItem[]
  closeRemark: string
  closeSubmitting: boolean
}>()

const emit = defineEmits<{
  review: []
  close: []
  'update:closeRemark': [value: string]
}>()

const judgeOptions: { label: string; value: ResultJudge; color: string }[] = [
  { label: '通过', value: 'PASS', color: 'success' },
  { label: '不通过', value: 'FAIL', color: 'error' },
  { label: '待复核', value: 'RECHECK', color: 'warning' },
]

const getJudgeMeta = (value?: string) => judgeOptions.find((item) => item.value === value)

const getAttachmentRole = (item: InspectionDataItem) => {
  if (isLikelyResultImage(item.filePath) || item.fileType.includes('result')) {
    return { label: '结果图', color: 'blue' }
  }
  if (isLikelySourceImage(item.filePath) || item.fileType === 'image' || item.fileType === 'frame') {
    return { label: '原图', color: 'green' }
  }
  return { label: item.fileType, color: 'default' }
}

const boxSummaryText = computed(() => {
  if (props.currentBoxes.length) {
    return `已返回 ${props.currentBoxes.length} 个检测框`
  }
  if (props.currentJudge === 'PASS') {
    return '当前结果判定为 PASS，后端未返回瑕疵框'
  }
  return '当前结果未返回 boxes 数据，请检查后端识别结果'
})
</script>

<template>
  <a-spin :spinning="detailLoading">
    <template v-if="activeRecord || detectionResult">
      <div class="result-overview">
        <div class="judge-block">
          <span class="label">判定结果</span>
          <a-tag :color="getJudgeMeta(currentJudge)?.color">
            {{ getJudgeMeta(currentJudge)?.label || currentJudge || '-' }}
          </a-tag>
        </div>
        <div class="metric">
          <span>置信度</span>
          <strong>{{ currentConfidence ?? '-' }}</strong>
        </div>
        <div class="metric">
          <span>缺陷类型</span>
          <strong>{{ currentDefectType || '-' }}</strong>
        </div>
        <div class="metric">
          <span>结果值</span>
          <strong>{{ currentResultValue || '-' }}</strong>
        </div>
      </div>

      <div class="image-grid">
        <a-card size="small" title="原图预览">
          <div class="image-frame">
            <a-image v-if="sourcePreviewUrl" :src="sourcePreviewUrl" alt="原图预览" />
            <a-empty v-else description="未返回原图路径" />
          </div>
          <div class="path-text">{{ normalizeImagePath(sourceImagePath) || '等待 Java 返回 sourceImageUrl' }}</div>
        </a-card>
        <a-card size="small" title="结果图预览">
          <div class="image-frame">
            <a-image v-if="resultPreviewUrl" :src="resultPreviewUrl" alt="结果图预览" />
            <a-empty v-else description="未返回结果图路径" />
          </div>
          <div class="path-text">{{ normalizeImagePath(resultImagePath) || '等待 Java 返回 imageUrl' }}</div>
        </a-card>
      </div>

      <a-card size="small" title="检测结果详情" class="inner-card">
        <a-descriptions :column="1" size="small" bordered>
          <a-descriptions-item label="工序">
            {{ activePlanStep?.stepName || activeRecord?.planStepId || detectionResult?.planStepId || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="设备">
            {{ activePlanStep?.machineName || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="记录ID">
            {{ activeRecord?.inspectionId || detectionResult?.inspectionId || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="检测方式">
            {{ activeRecord?.inspectType || detectionResult?.inspectType || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="备注">
            {{ activeRecord?.remark || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="框线结果">
            {{ boxSummaryText }}
          </a-descriptions-item>
        </a-descriptions>
      </a-card>

      <a-card size="small" title="结构化 boxes 数据" class="inner-card">
        <a-table
          v-if="currentBoxes.length"
          :columns="[
            { title: '缺陷', dataIndex: 'label', key: 'label' },
            { title: '置信度', dataIndex: 'score', key: 'score' },
            { title: 'x1', dataIndex: 'x1', key: 'x1' },
            { title: 'y1', dataIndex: 'y1', key: 'y1' },
            { title: 'x2', dataIndex: 'x2', key: 'x2' },
            { title: 'y2', dataIndex: 'y2', key: 'y2' },
          ]"
          :data-source="currentBoxes"
          :pagination="false"
          size="small"
          row-key="label"
        />
        <a-empty v-else :description="boxSummaryText" />
      </a-card>

      <a-card size="small" title="附件证据" class="inner-card">
        <template v-if="activeAttachments.length">
          <div v-for="item in activeAttachments" :key="item.dataId" class="attachment-item">
            <div class="attachment-head">
              <strong>{{ item.fileName }}</strong>
              <a-tag :color="getAttachmentRole(item).color">{{ getAttachmentRole(item).label }}</a-tag>
            </div>
            <div class="path-text">{{ normalizeImagePath(item.filePath) }}</div>
            <div class="attachment-meta">采集时间：{{ formatDateTime(item.captureTime) }}</div>
            <div class="attachment-meta">结果摘要：{{ item.resultSummary || '-' }}</div>
          </div>
        </template>
        <a-empty v-else description="暂无附件证据" />
      </a-card>

      <div class="result-actions">
        <a-button type="primary" ghost :disabled="!activeRecord" @click="emit('review')">人工复核</a-button>
        <a-input
          :value="closeRemark"
          placeholder="可填写关闭说明"
          :disabled="!activeRecord"
          class="close-input"
          @update:value="emit('update:closeRemark', $event)"
        />
        <a-button :loading="closeSubmitting" :disabled="!activeRecord" @click="emit('close')">关闭记录</a-button>
      </div>
    </template>

    <a-empty v-else description="完成检测后，将在这里展示 Java 返回的识别结果" />
  </a-spin>
</template>

<style scoped>
.result-overview {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.judge-block,
.metric {
  min-height: 74px;
  padding: 14px;
  border: 1px solid rgba(145, 158, 171, 0.16);
  border-radius: 12px;
  background: #f8fafc;
}

.judge-block {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.metric span,
.label {
  display: block;
  margin-bottom: 8px;
  color: #667085;
}

.metric strong {
  color: #1f2937;
  font-size: 18px;
}

.image-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.image-frame {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 260px;
  margin-top: 12px;
  border: 1px dashed rgba(145, 158, 171, 0.35);
  border-radius: 12px;
  background: #f8fafc;
  overflow: hidden;
}

:deep(.image-frame img) {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.path-text {
  margin-top: 8px;
  color: #667085;
  font-size: 12px;
  line-height: 1.5;
  word-break: break-all;
}

.inner-card {
  margin-top: 16px;
}

.attachment-item {
  padding: 10px 0;
  border-bottom: 1px solid rgba(145, 158, 171, 0.12);
}

.attachment-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.attachment-meta {
  margin-top: 4px;
  color: #667085;
  font-size: 13px;
}

.result-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
}

.close-input {
  flex: 1;
}

@media (max-width: 1280px) {
  .image-grid {
    grid-template-columns: 1fr;
  }

  .result-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .result-actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
