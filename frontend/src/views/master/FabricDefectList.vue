<script setup lang="ts">
import { reactive, onMounted, ref } from 'vue'
import type { UploadFile } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import TablePage from '@/components/TablePage.vue'
import SearchBar from '@/components/SearchBar.vue'
import { useTable } from '@/hooks/useTable'
import { PlusOutlined, UploadOutlined } from '@ant-design/icons-vue'

type DefectItem = {
  key: string
  code: string
  name: string
  category: string
  description: string
  severity: string
  datasetName?: string
  tuningStatus?: string
}

const categoryOptions = [
  { label: '经向瑕疵', value: '经向瑕疵' },
  { label: '纬向瑕疵', value: '纬向瑕疵' },
  { label: '染整瑕疵', value: '染整瑕疵' },
  { label: '织造瑕疵', value: '织造瑕疵' },
  { label: '外观瑕疵', value: '外观瑕疵' },
]

const severityOptions = [
  { label: '严重', value: '严重' },
  { label: '中等', value: '中等' },
  { label: '轻微', value: '轻微' },
]

const searchForm = reactive({
  keyword: '',
  category: undefined as string | undefined,
  severity: undefined as string | undefined,
})

const searchFields = [
  {
    label: '关键词',
    name: 'keyword',
    type: 'input' as const,
    placeholder: '瑕疵编码/名称',
    width: '200px',
  },
  {
    label: '分类',
    name: 'category',
    type: 'select' as const,
    placeholder: '选择分类',
    options: categoryOptions,
  },
  {
    label: '严重程度',
    name: 'severity',
    type: 'select' as const,
    placeholder: '选择严重程度',
    options: severityOptions,
  },
]

const columns = [
  { title: '瑕疵编码', dataIndex: 'code', key: 'code', width: '120px' },
  { title: '瑕疵名称', dataIndex: 'name', key: 'name', width: '120px' },
  { title: '分类', dataIndex: 'category', key: 'category', width: '120px' },
  { title: '描述', dataIndex: 'description', key: 'description' },
  { title: '严重程度', dataIndex: 'severity', key: 'severity', width: '120px' },
  { title: '微调数据集', dataIndex: 'datasetName', key: 'datasetName', width: '180px' },
  { title: '微调状态', dataIndex: 'tuningStatus', key: 'tuningStatus', width: '120px' },
]

const { data, loading, pagination } = useTable<DefectItem>()

const allData: DefectItem[] = [
  {
    key: '1',
    code: 'DF-001',
    name: '油污',
    category: '染整瑕疵',
    description: '布面沾染机油、润滑油等油类物质形成的污渍，呈深色斑块状',
    severity: '严重',
  },
  {
    key: '2',
    code: 'DF-002',
    name: '断经',
    category: '经向瑕疵',
    description: '经纱在织造过程中断裂，导致布面出现纵向缺纱痕迹',
    severity: '严重',
  },
  {
    key: '3',
    code: 'DF-003',
    name: '断纬',
    category: '纬向瑕疵',
    description: '纬纱在织造过程中断裂，导致布面出现横向缺纱痕迹',
    severity: '严重',
  },
  {
    key: '4',
    code: 'DF-004',
    name: '破洞',
    category: '织造瑕疵',
    description: '布面出现孔洞，通常由纱线断裂或外力损伤造成',
    severity: '严重',
  },
  {
    key: '5',
    code: 'DF-005',
    name: '色差',
    category: '染整瑕疵',
    description: '同一批布料颜色存在明显差异，与标准色样不符',
    severity: '中等',
  },
  {
    key: '6',
    code: 'DF-006',
    name: '纬斜',
    category: '纬向瑕疵',
    description: '纬纱与经纱不垂直，布面纹路呈倾斜状态',
    severity: '中等',
  },
  {
    key: '7',
    code: 'DF-007',
    name: '稀密路',
    category: '纬向瑕疵',
    description: '纬纱密度不均匀，呈现局部过稀或过密的横路条纹',
    severity: '中等',
  },
  {
    key: '8',
    code: 'DF-008',
    name: '粗经',
    category: '经向瑕疵',
    description: '经纱中混入偏粗纱线，布面出现纵向明显粗条纹',
    severity: '轻微',
  },
  {
    key: '9',
    code: 'DF-009',
    name: '粗纬',
    category: '纬向瑕疵',
    description: '纬纱中混入偏粗纱线，布面出现横向明显粗条纹',
    severity: '轻微',
  },
  {
    key: '10',
    code: 'DF-010',
    name: '跳花',
    category: '织造瑕疵',
    description: '纱线未按组织规律交织，多根纱线浮于布面形成花纹状瑕疵',
    severity: '中等',
  },
  {
    key: '11',
    code: 'DF-011',
    name: '跳纱',
    category: '织造瑕疵',
    description: '单根纱线未按规律交织，跳过应交织的纱线，形成局部松散',
    severity: '中等',
  },
  {
    key: '12',
    code: 'DF-012',
    name: '蛛网',
    category: '织造瑕疵',
    description: '多根经纱或纬纱断裂后纠缠成网状的局部瑕疵区域',
    severity: '严重',
  },
  {
    key: '13',
    code: 'DF-013',
    name: '边不良',
    category: '织造瑕疵',
    description: '布边出现松边、紧边、烂边、荷叶边等不规整现象',
    severity: '轻微',
  },
  {
    key: '14',
    code: 'DF-014',
    name: '折痕',
    category: '外观瑕疵',
    description: '布面因折叠或挤压产生的不可恢复的痕迹，影响平整度',
    severity: '轻微',
  },
  {
    key: '15',
    code: 'DF-015',
    name: '污渍',
    category: '外观瑕疵',
    description: '布面沾染泥渍、锈渍、色渍等非油性污物形成的斑点',
    severity: '中等',
  },
]

const loadData = () => {
  loading.value = true
  setTimeout(() => {
    data.value = allData.filter((item) => {
      const hitKeyword =
        !searchForm.keyword ||
        item.code.includes(searchForm.keyword) ||
        item.name.includes(searchForm.keyword)
      const hitCategory = !searchForm.category || item.category === searchForm.category
      const hitSeverity = !searchForm.severity || item.severity === searchForm.severity
      return hitKeyword && hitCategory && hitSeverity
    })
    pagination.total = data.value.length
    loading.value = false
  }, 200)
}

const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.category = undefined
  searchForm.severity = undefined
  loadData()
}

onMounted(loadData)

const modalOpen = ref(false)
const datasetFiles = ref<UploadFile[]>([])

const defectForm = reactive({
  code: '',
  name: '',
  category: undefined as string | undefined,
  severity: undefined as string | undefined,
  description: '',
})

const resetDefectForm = () => {
  Object.assign(defectForm, {
    code: `DF-${String(allData.length + 1).padStart(3, '0')}`,
    name: '',
    category: undefined,
    severity: undefined,
    description: '',
  })
  datasetFiles.value = []
}

const openCreateModal = () => {
  resetDefectForm()
  modalOpen.value = true
}

const beforeDatasetUpload = () => false

const handleCreateDefect = () => {
  if (
    !defectForm.code ||
    !defectForm.name ||
    !defectForm.category ||
    !defectForm.severity ||
    !defectForm.description
  ) {
    message.warning('请完整填写缺陷类型信息')
    return
  }

  const dataset = datasetFiles.value[0]
  if (!dataset) {
    message.warning('新增布料缺陷类型必须上传数据集用于微调')
    return
  }

  allData.unshift({
    key: String(allData.length + 1),
    code: defectForm.code,
    name: defectForm.name,
    category: defectForm.category,
    description: defectForm.description,
    severity: defectForm.severity,
    datasetName: dataset.name,
    tuningStatus: '待微调',
  })

  loadData()
  modalOpen.value = false
  message.success('已新增缺陷类型，数据集已进入微调队列（示例）')
}

const severityColorMap: Record<string, string> = {
  严重: 'red',
  中等: 'orange',
  轻微: 'default',
}

const tuningStatusColorMap: Record<string, string> = {
  待微调: 'blue',
}
</script>

<template>
  <TablePage
    title="布料瑕疵种类"
    :columns="columns"
    :data="data"
    :loading="loading"
    :pagination="pagination"
  >
    <template #search>
      <SearchBar
        :model="searchForm"
        :fields="searchFields"
        @search="loadData"
        @reset="resetSearch"
      />
    </template>

    <template #actions>
      <a-button type="primary" class="add-btn" @click="openCreateModal">
        <template #icon><PlusOutlined /></template>
        新增缺陷类型
      </a-button>
    </template>

    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'severity'">
        <a-tag :color="severityColorMap[record.severity]">
          {{ record.severity }}
        </a-tag>
      </template>
      <template v-else-if="column.key === 'datasetName'">
        <span v-if="record.datasetName">{{ record.datasetName }}</span>
        <span v-else class="empty-text">未上传</span>
      </template>
      <template v-else-if="column.key === 'tuningStatus'">
        <a-tag v-if="record.tuningStatus" :color="tuningStatusColorMap[record.tuningStatus]">
          {{ record.tuningStatus }}
        </a-tag>
        <span v-else class="empty-text">-</span>
      </template>
    </template>
  </TablePage>

  <a-modal
    v-model:open="modalOpen"
    title="新增布料缺陷类型"
    ok-text="提交并微调"
    cancel-text="取消"
    width="680px"
    @ok="handleCreateDefect"
  >
    <a-alert
      class="dataset-alert"
      type="info"
      show-icon
      message="新增缺陷类型前必须上传标注数据集，系统将使用该数据集进行模型微调。"
    />

    <a-form layout="vertical" class="defect-form">
      <div class="form-grid">
        <a-form-item label="瑕疵编码" required>
          <a-input v-model:value="defectForm.code" placeholder="例如：DF-016" />
        </a-form-item>
        <a-form-item label="瑕疵名称" required>
          <a-input v-model:value="defectForm.name" placeholder="请输入缺陷名称" />
        </a-form-item>
        <a-form-item label="分类" required>
          <a-select
            v-model:value="defectForm.category"
            :options="categoryOptions"
            placeholder="请选择分类"
          />
        </a-form-item>
        <a-form-item label="严重程度" required>
          <a-select
            v-model:value="defectForm.severity"
            :options="severityOptions"
            placeholder="请选择严重程度"
          />
        </a-form-item>
      </div>

      <a-form-item label="描述" required>
        <a-textarea
          v-model:value="defectForm.description"
          :rows="3"
          placeholder="描述该缺陷的视觉特征、成因或判定标准"
        />
      </a-form-item>

      <a-form-item label="微调数据集" required>
        <a-upload-dragger
          v-model:file-list="datasetFiles"
          name="dataset"
          :max-count="1"
          accept=".zip,.rar,.7z,.tar,.gz,.json,.yaml,.yml"
          :before-upload="beforeDatasetUpload"
        >
          <p class="upload-icon">
            <UploadOutlined />
          </p>
          <p class="upload-title">上传标注数据集</p>
          <p class="upload-hint">支持 YOLO 标注压缩包或配置文件，当前为静态展示，不会真实上传。</p>
        </a-upload-dragger>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped>
.add-btn {
  background-color: #ff7a45;
  border-color: #ff7a45;
}

.add-btn:hover {
  background-color: #ff9c6e;
  border-color: #ff9c6e;
}

.dataset-alert {
  margin-bottom: 16px;
}

.defect-form {
  margin-top: 4px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.upload-icon {
  margin-bottom: 6px;
  color: #ff7a45;
  font-size: 28px;
}

.upload-title {
  margin-bottom: 4px;
  color: #1e293b;
  font-weight: 700;
}

.upload-hint {
  margin-bottom: 0;
  color: #64748b;
}

.empty-text {
  color: #94a3b8;
}

:deep(.ant-table-thead > tr > th) {
  background-color: #ffffff;
  border-bottom: 1px solid #f1f5f9;
}

:deep(.ant-table-tbody > tr > td) {
  border-bottom: 1px solid #f1f5f9;
}
</style>
