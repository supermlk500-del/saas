<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { closeQcRecord, fetchQcRecords, getQcRecord, reviewQcRecord, type QcRecordCloseRequest, type QcRecordQuery, type QcRecordReviewRequest } from '@/api/quality/qcRecord'
import { fetchInspectionData } from '@/api/quality/inspectionData'
import { fetchQcItems } from '@/api/quality/qcItem'
import TablePage from '@/components/TablePage.vue'
import SearchBar from '@/components/SearchBar.vue'
import { useTable } from '@/hooks/useTable'
import type { IdValue, InspectionDataItem, QcDetectionBox, QcItem, QcRecordItem } from '@/types/domain'
import type { InspectType, ResultJudge } from '@/types/dictionary'
import { formatDateTime } from '@/utils/date'
import { formatInspectionId, formatPlanStepId } from '@/utils/idFormat'
import { isLikelyResultImage, isLikelySourceImage, normalizeImagePath, resolveImageUrl } from '@/utils/image'

const judgeOptions: { label: string; value: ResultJudge; color: string }[] = [
  { label: '通过', value: 'PASS', color: 'success' },
  { label: '不通过', value: 'FAIL', color: 'error' },
  { label: '待复核', value: 'RECHECK', color: 'warning' },
]

const inspectTypeOptions: { label: string; value: InspectType }[] = [
  { label: '图片检测', value: 'offline' },
  { label: '视频检测', value: 'video' },
]

const searchForm = reactive({
  planStepId: undefined as IdValue | undefined,
  qcItemId: undefined as IdValue | undefined,
  inspectType: undefined as InspectType | undefined,
  resultJudge: undefined as ResultJudge | undefined,
  inspectTimeFrom: '',
  inspectTimeTo: '',
})

const qcItems = ref<QcItem[]>([])
const qcItemSearchOptions = computed(() =>
  qcItems.value.map((item) => ({
    label: `${item.qcItemCode} / ${item.qcItemName}`,
    value: item.qcItemId,
  })),
)

const searchFields = computed(() => [
  { label: '工序计划ID', name: 'planStepId', type: 'number' as const, placeholder: '请输入工序计划ID', width: '180px' },
  { label: '质检项', name: 'qcItemId', type: 'select' as const, placeholder: '全部', options: qcItemSearchOptions.value, width: '220px' },
  { label: '检测方式', name: 'inspectType', type: 'select' as const, placeholder: '全部', options: inspectTypeOptions, width: '150px' },
  { label: '判定结果', name: 'resultJudge', type: 'select' as const, placeholder: '全部', options: judgeOptions, width: '150px' },
  { label: '开始时间', name: 'inspectTimeFrom', type: 'datetime' as const, width: '220px' },
  { label: '结束时间', name: 'inspectTimeTo', type: 'datetime' as const, width: '220px' },
])

const columns = [
  { title: '记录ID', dataIndex: 'inspectionId', key: 'inspectionId', width: 84 },
  { title: '工序计划ID', dataIndex: 'planStepId', key: 'planStepId', width: 96 },
  { title: '质检项', dataIndex: 'qcItemId', key: 'qcItemId', width: 156 },
  { title: '检测方式', dataIndex: 'inspectType', key: 'inspectType', width: 92 },
  { title: '检测信息', key: 'detectionMeta', width: 122 },
  { title: '检测结果', key: 'resultSummary', width: 180 },
  { title: '检验信息', key: 'inspectionMeta', width: 168 },
  { title: '操作', key: 'action', width: 88 },
]

const { data, loading, pagination } = useTable<QcRecordItem>()

const detailDrawerOpen = ref(false)
const detailLoading = ref(false)
const detailRecord = ref<QcRecordItem | null>(null)
const attachmentList = ref<InspectionDataItem[]>([])

const reviewModalOpen = ref(false)
const reviewSubmitting = ref(false)
const reviewFormRef = ref()
const reviewForm = reactive({
  inspectionId: undefined as IdValue | undefined,
  reviewResult: undefined as ResultJudge | undefined,
  reviewer: '',
  reviewRemark: '',
})

const closeModalOpen = ref(false)
const closeSubmitting = ref(false)
const closeForm = reactive({
  inspectionId: undefined as IdValue | undefined,
  closeRemark: '',
})

const reviewRules = {
  reviewResult: [{ required: true, message: '请选择复核结果' }],
}

const getJudgeMeta = (value?: string) =>
  judgeOptions.find((item) => item.value === value)

const getInspectTypeLabel = (value?: string) =>
  inspectTypeOptions.find((item) => item.value === value)?.label || value || '-'

const getQcItemLabel = (qcItemId?: IdValue) => {
  const item = qcItems.value.find((current) => String(current.qcItemId) === String(qcItemId))
  return item ? `${item.qcItemCode} / ${item.qcItemName}` : qcItemId || '-'
}

const attachmentRole = (item: InspectionDataItem) => {
  if (isLikelyResultImage(item.filePath) || item.fileType.includes('result')) {
    return { label: '结果图', color: 'blue' }
  }
  if (isLikelySourceImage(item.filePath)) {
    return { label: '原图', color: 'green' }
  }
  return { label: item.fileType, color: 'default' }
}

const sourceAttachment = computed(() =>
  attachmentList.value.find((item) => isLikelySourceImage(item.filePath)) ?? attachmentList.value.find((item) => item.fileType === 'image' || item.fileType === 'frame'),
)

const resultAttachment = computed(() =>
  attachmentList.value.find((item) => isLikelyResultImage(item.filePath) || item.fileType.includes('result')),
)

const sourceImagePath = computed(() => detailRecord.value?.sourceImageUrl || sourceAttachment.value?.filePath || '')
const resultImagePath = computed(() => detailRecord.value?.imageUrl || resultAttachment.value?.filePath || '')
const sourcePreviewUrl = computed(() => resolveImageUrl(sourceImagePath.value))
const resultPreviewUrl = computed(() => resolveImageUrl(resultImagePath.value))

const parsedBoxes = computed<QcDetectionBox[]>(() => {
  const value = detailRecord.value?.resultValue || ''
  if (!value.trim().startsWith('[')) {
    return []
  }
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
})

const loadQcItems = async () => {
  const response = await fetchQcItems({ pageNum: 1, pageSize: 200 })
  qcItems.value = response.list
}

const loadData = async () => {
  loading.value = true
  try {
    const query: QcRecordQuery = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      planStepId: searchForm.planStepId,
      qcItemId: searchForm.qcItemId,
      inspectType: searchForm.inspectType,
      resultJudge: searchForm.resultJudge,
      inspectTimeFrom: searchForm.inspectTimeFrom || undefined,
      inspectTimeTo: searchForm.inspectTimeTo || undefined,
    }
    const response = await fetchQcRecords(query)
    data.value = response.list
    pagination.total = response.total
  } finally {
    loading.value = false
  }
}

pagination.onChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  void loadData()
}

const resetSearch = () => {
  searchForm.planStepId = undefined
  searchForm.qcItemId = undefined
  searchForm.inspectType = undefined
  searchForm.resultJudge = undefined
  searchForm.inspectTimeFrom = ''
  searchForm.inspectTimeTo = ''
  pagination.current = 1
  void loadData()
}

const openDetailDrawer = async (record: QcRecordItem) => {
  detailDrawerOpen.value = true
  detailLoading.value = true
  try {
    const [detail, attachments] = await Promise.all([
      getQcRecord(record.inspectionId),
      fetchInspectionData({ pageNum: 1, pageSize: 100, qcRecordId: record.inspectionId }),
    ])
    detailRecord.value = detail.data
    attachmentList.value = attachments.list
  } finally {
    detailLoading.value = false
  }
}

const openReviewModal = (record: QcRecordItem) => {
  reviewForm.inspectionId = record.inspectionId
  reviewForm.reviewResult = record.resultJudge
  reviewForm.reviewer = ''
  reviewForm.reviewRemark = ''
  reviewModalOpen.value = true
}

const openCloseModal = (record: QcRecordItem) => {
  closeForm.inspectionId = record.inspectionId
  closeForm.closeRemark = ''
  closeModalOpen.value = true
}

const handleReviewSubmit = async () => {
  await reviewFormRef.value?.validate()
  reviewSubmitting.value = true
  try {
    const payload: QcRecordReviewRequest = {
      reviewResult: reviewForm.reviewResult ?? 'PASS',
      reviewer: reviewForm.reviewer.trim() || undefined,
      reviewRemark: reviewForm.reviewRemark.trim() || undefined,
    }
    await reviewQcRecord(reviewForm.inspectionId ?? 0, payload)
    reviewModalOpen.value = false
    message.success('复核已保存')
    await loadData()
    const currentDetail = detailRecord.value
    if (currentDetail && String(currentDetail.inspectionId) === String(reviewForm.inspectionId)) {
      await openDetailDrawer(currentDetail)
    }
  } finally {
    reviewSubmitting.value = false
  }
}

const handleCloseSubmit = async () => {
  closeSubmitting.value = true
  try {
    const payload: QcRecordCloseRequest = {
      closeRemark: closeForm.closeRemark.trim() || undefined,
    }
    await closeQcRecord(closeForm.inspectionId ?? 0, payload)
    closeModalOpen.value = false
    message.success('质检记录已关闭')
    await loadData()
    const currentDetail = detailRecord.value
    if (currentDetail && String(currentDetail.inspectionId) === String(closeForm.inspectionId)) {
      await openDetailDrawer(currentDetail)
    }
  } finally {
    closeSubmitting.value = false
  }
}

onMounted(async () => {
  await loadQcItems()
  await loadData()
})
</script>

<template>
  <TablePage
    title="质检结果"
    :columns="columns"
    :data="data"
    :loading="loading"
    :pagination="pagination"
    row-key="inspectionId"
    table-layout="fixed"
    class="iqc-result-page"
  >
    <template #search>
      <SearchBar :model="searchForm" :fields="searchFields" @search="loadData" @reset="resetSearch" />
    </template>

    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'inspectionId'">
        <a-tooltip :title="String(record.inspectionId)">
          {{ formatInspectionId(record.inspectionId) }}
        </a-tooltip>
      </template>
      <template v-else-if="column.key === 'planStepId'">
        <a-tooltip :title="String(record.planStepId)">
          {{ formatPlanStepId(record.planStepId) }}
        </a-tooltip>
      </template>
      <template v-else-if="column.key === 'qcItemId'">
        <a-tooltip :title="getQcItemLabel(record.qcItemId)" placement="topLeft">
          <span class="single-line-cell">{{ getQcItemLabel(record.qcItemId) }}</span>
        </a-tooltip>
      </template>
      <template v-else-if="column.key === 'inspectType'">
        {{ getInspectTypeLabel(record.inspectType) }}
      </template>
      <template v-else-if="column.key === 'detectionMeta'">
        <div class="stacked-meta">
          <span><em>摄像头</em>{{ record.cameraId ?? '-' }}</span>
          <span><em>置信度</em>{{ record.confidenceScore ?? '-' }}</span>
        </div>
      </template>
      <template v-else-if="column.key === 'resultSummary'">
        <div class="result-summary-cell">
          <a-tooltip :title="record.resultValue || '-'">
            <span class="result-value">{{ record.resultValue || '-' }}</span>
          </a-tooltip>
          <a-tag :color="getJudgeMeta(record.resultJudge)?.color">
            {{ getJudgeMeta(record.resultJudge)?.label || record.resultJudge || '-' }}
          </a-tag>
        </div>
      </template>
      <template v-else-if="column.key === 'inspectionMeta'">
        <div class="inspection-meta">
          <strong>{{ record.inspector || '未记录检验人' }}</strong>
          <a-tooltip :title="formatDateTime(record.inspectTime)">
            <span>{{ formatDateTime(record.inspectTime) }}</span>
          </a-tooltip>
        </div>
      </template>
      <template v-else-if="column.key === 'action'">
        <div class="result-actions">
          <a-button type="link" size="small" @click="openDetailDrawer(record)">详情</a-button>
          <a-button type="link" size="small" @click="openReviewModal(record)">复核</a-button>
          <a-button type="link" size="small" @click="openCloseModal(record)">关闭</a-button>
        </div>
      </template>
    </template>
  </TablePage>

  <a-drawer v-model:open="detailDrawerOpen" title="质检结果详情" width="960px">
    <a-spin :spinning="detailLoading">
      <div v-if="detailRecord" class="detail-layout">
        <a-descriptions :column="2" bordered size="small">
          <a-descriptions-item label="记录ID">
            <a-tooltip :title="String(detailRecord.inspectionId)">
              {{ formatInspectionId(detailRecord.inspectionId) }}
            </a-tooltip>
          </a-descriptions-item>
          <a-descriptions-item label="工序计划ID">
            <a-tooltip :title="String(detailRecord.planStepId)">
              {{ formatPlanStepId(detailRecord.planStepId) }}
            </a-tooltip>
          </a-descriptions-item>
          <a-descriptions-item label="质检项">{{ getQcItemLabel(detailRecord.qcItemId) }}</a-descriptions-item>
          <a-descriptions-item label="检测方式">{{ getInspectTypeLabel(detailRecord.inspectType) }}</a-descriptions-item>
          <a-descriptions-item label="摄像头ID">{{ detailRecord.cameraId ?? '-' }}</a-descriptions-item>
          <a-descriptions-item label="帧时间">{{ formatDateTime(detailRecord.frameTime) }}</a-descriptions-item>
          <a-descriptions-item label="置信度">{{ detailRecord.confidenceScore ?? '-' }}</a-descriptions-item>
          <a-descriptions-item label="结果值">{{ detailRecord.resultValue || '-' }}</a-descriptions-item>
          <a-descriptions-item label="判定结果">
            <a-tag :color="getJudgeMeta(detailRecord.resultJudge)?.color">
              {{ getJudgeMeta(detailRecord.resultJudge)?.label || detailRecord.resultJudge }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="检验人">{{ detailRecord.inspector || '-' }}</a-descriptions-item>
          <a-descriptions-item label="检测时间">{{ formatDateTime(detailRecord.inspectTime) }}</a-descriptions-item>
          <a-descriptions-item label="备注">{{ detailRecord.remark || '-' }}</a-descriptions-item>
        </a-descriptions>

        <div class="preview-grid">
          <a-card title="原图" size="small">
            <div class="image-frame">
              <a-image v-if="sourcePreviewUrl" :src="sourcePreviewUrl" alt="原图" />
              <a-empty v-else description="未返回原图路径" />
            </div>
            <div class="path-text">{{ normalizeImagePath(sourceImagePath) || '-' }}</div>
            <div class="hint-text">图片无法预览时，请确认 Java 是否已开放相对路径静态访问。</div>
          </a-card>

          <a-card title="结果图" size="small">
            <div class="image-frame">
              <a-image v-if="resultPreviewUrl" :src="resultPreviewUrl" alt="结果图" />
              <a-empty v-else description="未返回结果图路径" />
            </div>
            <div class="path-text">{{ normalizeImagePath(resultImagePath) || '-' }}</div>
            <div class="hint-text">结果图通常来自 imageUrl 或 photo/results 附件。</div>
          </a-card>
        </div>

        <a-card title="附件证据" size="small">
          <template v-if="attachmentList.length">
            <div v-for="item in attachmentList" :key="item.dataId" class="attachment-item">
              <div class="attachment-head">
                <strong>{{ item.fileName }}</strong>
                <a-tag :color="attachmentRole(item).color">{{ attachmentRole(item).label }}</a-tag>
              </div>
              <div class="path-text">{{ normalizeImagePath(item.filePath) }}</div>
              <div class="attachment-meta">采集时间：{{ formatDateTime(item.captureTime) }}</div>
              <div class="attachment-meta">结果摘要：{{ item.resultSummary || '-' }}</div>
              <div class="attachment-meta">备注：{{ item.remark || '-' }}</div>
              <a-image
                v-if="resolveImageUrl(item.filePath)"
                :src="resolveImageUrl(item.filePath)"
                :width="96"
                class="attachment-preview"
              />
            </div>
          </template>
          <a-empty v-else description="暂无附件证据" />
        </a-card>

        <a-card v-if="parsedBoxes.length" title="结构化检测框" size="small">
          <a-table
            :columns="[
              { title: '缺陷', dataIndex: 'label', key: 'label' },
              { title: '置信度', dataIndex: 'score', key: 'score' },
              { title: 'x1', dataIndex: 'x1', key: 'x1' },
              { title: 'y1', dataIndex: 'y1', key: 'y1' },
              { title: 'x2', dataIndex: 'x2', key: 'x2' },
              { title: 'y2', dataIndex: 'y2', key: 'y2' },
            ]"
            :data-source="parsedBoxes"
            :pagination="false"
            size="small"
          />
        </a-card>
      </div>
    </a-spin>
  </a-drawer>

  <a-modal
    v-model:open="reviewModalOpen"
    title="人工复核"
    ok-text="保存"
    cancel-text="取消"
    :confirm-loading="reviewSubmitting"
    width="560px"
    @ok="handleReviewSubmit"
  >
    <a-form ref="reviewFormRef" :model="reviewForm" :rules="reviewRules" layout="vertical">
      <a-form-item label="复核结果" name="reviewResult">
        <a-select v-model:value="reviewForm.reviewResult" :options="judgeOptions" placeholder="请选择复核结果" />
      </a-form-item>
      <a-form-item label="复核人" name="reviewer">
        <a-input v-model:value="reviewForm.reviewer" placeholder="请输入复核人" />
      </a-form-item>
      <a-form-item label="复核说明" name="reviewRemark">
        <a-textarea v-model:value="reviewForm.reviewRemark" :rows="3" placeholder="请输入复核说明" />
      </a-form-item>
    </a-form>
  </a-modal>

  <a-modal
    v-model:open="closeModalOpen"
    title="关闭质检记录"
    ok-text="关闭"
    cancel-text="取消"
    :confirm-loading="closeSubmitting"
    width="520px"
    @ok="handleCloseSubmit"
  >
    <a-form :model="closeForm" layout="vertical">
      <a-form-item label="关闭说明" name="closeRemark">
        <a-textarea v-model:value="closeForm.closeRemark" :rows="3" placeholder="请输入关闭说明" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped>
.iqc-result-page :deep(.ant-table-cell) {
  overflow-wrap: normal;
  word-break: normal;
}

.iqc-result-page :deep(.ant-table-content) {
  overflow-x: clip !important;
}

.iqc-result-page :deep(.ant-table-tbody > tr > td) {
  padding-top: 12px;
  padding-bottom: 12px;
  vertical-align: middle;
}

.single-line-cell,
.result-value {
  display: block;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.stacked-meta,
.inspection-meta {
  display: grid;
  gap: 4px;
  font-size: 12px;
}

.stacked-meta span {
  display: grid;
  grid-template-columns: 50px 1fr;
}

.stacked-meta em {
  color: #94a3b8;
  font-style: normal;
}

.result-summary-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}

.result-summary-cell .ant-tag {
  margin-inline-end: 0;
}

.inspection-meta strong,
.inspection-meta span {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.inspection-meta span {
  color: #64748b;
}

.result-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 6px;
}

.result-actions :deep(.ant-btn) {
  height: 26px;
  padding-inline: 0;
}

.detail-layout {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.preview-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.image-frame {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 280px;
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

.hint-text {
  margin-top: 4px;
  color: #98a2b3;
  font-size: 12px;
}

.attachment-item {
  position: relative;
  padding: 12px 112px 12px 0;
  border-bottom: 1px solid rgba(145, 158, 171, 0.12);
}

.attachment-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.attachment-meta {
  margin-top: 4px;
  color: #667085;
  font-size: 13px;
  line-height: 1.6;
}

.attachment-preview {
  position: absolute;
  top: 12px;
  right: 0;
}

@media (max-width: 900px) {
  .preview-grid {
    grid-template-columns: 1fr;
  }

  .attachment-item {
    padding-right: 0;
  }

  .attachment-preview {
    position: static;
    margin-top: 10px;
  }
}
</style>
