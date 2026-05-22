<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import {
  PlusOutlined,
  FolderOutlined,
  FileTextOutlined,
  RocketOutlined,
  CodeSandboxOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons-vue'
import FormModal from '@/components/FormModal.vue'

interface FabricProcess {
  key: string
  code: string
  name: string
  warpDensity: number // 经密
  weftDensity: number // 纬密
  material: string // 成分
  width: number // 门幅
  compatibleLooms: string[] // 建议机型
  warpShrinkage: number // 经缩
  weftShrinkage: number // 纬缩
  category: string
}

const data = ref<FabricProcess[]>([
  {
    key: '1',
    code: 'C32x32/68x68',
    name: '32支全棉细布',
    warpDensity: 268,
    weftDensity: 268,
    material: '100% Cotton',
    width: 160,
    compatibleLooms: ['喷气织机', '剑杆织机'],
    warpShrinkage: 5,
    weftShrinkage: 3,
    category: 'cotton-plain',
  },
  {
    key: '2',
    code: 'C40x40/133x72',
    name: '40支全棉府绸',
    warpDensity: 524,
    weftDensity: 284,
    material: '100% Cotton',
    width: 150,
    compatibleLooms: ['喷气织机'],
    warpShrinkage: 4,
    weftShrinkage: 2,
    category: 'cotton-plain',
  },
])

const selectedKey = ref<string>('1')
const selectedItem = computed(() => data.value.find((item) => item.key === selectedKey.value))

const getLoomIcon = (type: string) => {
  switch (type) {
    case '喷气织机':
      return RocketOutlined
    case '剑杆织机':
      return CodeSandboxOutlined
    case '喷水织机':
      return ThunderboltOutlined
    default:
      return FileTextOutlined
  }
}

const treeData = [
  {
    title: '全棉系列',
    key: 'cotton',
    icon: FolderOutlined,
    children: [
      { title: '平纹', key: 'cotton-plain' },
      { title: '斜纹', key: 'cotton-twill' },
    ],
  },
  {
    title: '涤棉系列',
    key: 'tc',
    icon: FolderOutlined,
    children: [
      { title: 'TC 平纹', key: 'tc-plain' },
    ],
  },
  {
    title: '弹力系列',
    key: 'stretch',
    icon: FolderOutlined,
  },
]

const columns = [
  { title: '布种编号', dataIndex: 'code', key: 'code' },
  { title: '规格名称', dataIndex: 'name', key: 'name' },
  { title: '操作', key: 'action', width: '80px' },
]

const loading = ref(false)
const modalOpen = ref(false)
const formModel = reactive({
  code: '',
  name: '',
  warpDensity: 0,
  weftDensity: 0,
  material: '',
  width: 0,
  compatibleLooms: [] as string[],
  warpShrinkage: 0,
  weftShrinkage: 0,
})

const handleAdd = () => {
  modalOpen.value = true
  Object.assign(formModel, {
    code: '',
    name: '',
    warpDensity: 0,
    weftDensity: 0,
    material: '',
    width: 0,
    compatibleLooms: [],
    warpShrinkage: 0,
    weftShrinkage: 0,
  })
}

const handleOk = () => {
  const newItem = {
    key: Date.now().toString(),
    ...formModel,
    category: 'cotton-plain',
  }
  data.value.unshift(newItem as any)
  modalOpen.value = false
}

const onRowClick = (record: FabricProcess) => {
  selectedKey.value = record.key
}

const buildCustomRow = (record: FabricProcess) => ({
  onClick: () => onRowClick(record),
})
</script>

<template>
  <div class="fabric-process-page">
    <div class="page-header">
      <div class="title-section">
        <div class="title-dot"></div>
        <h2 class="page-title">工艺档案中心</h2>
      </div>
      <a-button type="primary" class="add-btn" @click="handleAdd">
        <template #icon><PlusOutlined /></template>
        新增规格
      </a-button>
    </div>

    <div class="content-layout">
      <!-- 左侧分类树 -->
      <a-card class="category-card" :bordered="false">
        <template #title>
          <div class="panel-title">布种分类</div>
        </template>
        <a-tree :tree-data="treeData" default-expand-all class="category-tree">
          <template #title="{ title }">
            <span>{{ title }}</span>
          </template>
        </a-tree>
      </a-card>

      <!-- 中间列表 -->
      <a-card class="list-card" :bordered="false">
        <a-table
          :columns="columns"
          :data-source="data"
          :loading="loading"
          class="custom-table"
          :row-selection="{ type: 'radio', selectedRowKeys: [selectedKey] }"
          :custom-row="buildCustomRow"
          :pagination="{ pageSize: 10 }"
        >
          <template #bodyCell="{ column }">
            <template v-if="column.key === 'action'">
              <a-button type="link" size="small" @click.stop="handleAdd">编辑</a-button>
            </template>
          </template>
        </a-table>
      </a-card>

      <!-- 右侧详情 -->
      <a-card class="detail-card" :bordered="false">
        <template #title>
          <div class="panel-title">布种工艺详情</div>
        </template>
        <div v-if="selectedItem" class="detail-content">
          <a-descriptions title="核心参数指标" bordered :column="1">
            <a-descriptions-item label="布种编号">
              <span class="highlight-text">{{ selectedItem.code }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="规格名称">{{ selectedItem.name }}</a-descriptions-item>
            <a-descriptions-item label="材料成分">{{ selectedItem.material }}</a-descriptions-item>
            <a-descriptions-item label="经向密度 (根/10cm)">{{ selectedItem.warpDensity }}</a-descriptions-item>
            <a-descriptions-item label="纬向密度 (根/10cm)">
              <span class="important-value">{{ selectedItem.weftDensity }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="门幅要求 (cm)">{{ selectedItem.width }}</a-descriptions-item>
            <a-descriptions-item label="经/纬缩率 (%)">
              {{ selectedItem.warpShrinkage }} / {{ selectedItem.weftShrinkage }}
            </a-descriptions-item>
          </a-descriptions>

          <div class="loom-section">
            <div class="sub-title">建议机型 (联动校验)</div>
            <div class="loom-grid">
              <div
                v-for="loom in selectedItem.compatibleLooms"
                :key="loom"
                class="loom-item"
              >
                <component :is="getLoomIcon(loom)" class="loom-icon" />
                <span>{{ loom }}</span>
              </div>
            </div>
          </div>
        </div>
        <a-empty v-else description="请从左侧选择一个布种规格" />
      </a-card>
    </div>

    <a-modal v-model:open="modalOpen" title="工艺参数设置" @ok="handleOk" width="700px">
      <a-form :model="formModel" layout="vertical">
        <div class="form-grid">
          <a-form-item label="布种编号" required>
            <a-input v-model:value="formModel.code" placeholder="如: CTN-32-68" />
          </a-form-item>
          <a-form-item label="规格名称" required>
            <a-input v-model:value="formModel.name" placeholder="如: 32支全棉细布" />
          </a-form-item>
          <a-form-item label="材料成分" required>
            <a-input v-model:value="formModel.material" placeholder="如: 100% Cotton" />
          </a-form-item>
          <a-form-item label="经向密度 (根/10cm)" required>
            <a-input-number v-model:value="formModel.warpDensity" style="width: 100%" />
          </a-form-item>
          <a-form-item label="纬向密度 (根/10cm)" required tooltip="核心参数！用于计算排产效率">
            <a-input-number v-model:value="formModel.weftDensity" style="width: 100%" />
          </a-form-item>
          <a-form-item label="门幅要求 (cm)" required>
            <a-input-number v-model:value="formModel.width" style="width: 100%" />
          </a-form-item>
          <a-form-item label="经缩率 (%)">
            <a-input-number v-model:value="formModel.warpShrinkage" style="width: 100%" />
          </a-form-item>
          <a-form-item label="纬缩率 (%)">
            <a-input-number v-model:value="formModel.weftShrinkage" style="width: 100%" />
          </a-form-item>
        </div>
        <a-form-item label="建议机型 (联动校验依据)">
          <a-checkbox-group v-model:value="formModel.compatibleLooms">
            <a-checkbox value="喷气织机">喷气织机</a-checkbox>
            <a-checkbox value="剑杆织机">剑杆织机</a-checkbox>
            <a-checkbox value="喷水织机">喷水织机</a-checkbox>
          </a-checkbox-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.fabric-process-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-dot {
  width: 4px;
  height: 18px;
  background: #ff7a45;
  border-radius: 2px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
}

.content-layout {
  display: grid;
  grid-template-columns: 220px 1.2fr 1.5fr;
  gap: 16px;
  align-items: start;
}

.category-card,
.list-card,
.detail-card {
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  height: calc(100vh - 180px);
  overflow-y: auto;
}

.panel-title {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}

.highlight-text {
  color: #ff7a45;
  font-weight: 700;
}

.important-value {
  color: #ff4d4f;
  font-weight: 700;
}

.loom-section {
  margin-top: 24px;
}

.sub-title {
  font-size: 14px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 12px;
}

.loom-grid {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.loom-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  min-width: 90px;
  transition: all 0.3s;
}

.loom-item:hover {
  border-color: #ff7a45;
  color: #ff7a45;
  background: #fff1e6;
}

.loom-icon {
  font-size: 24px;
}

.add-btn {
  background-color: #ff7a45;
  border-color: #ff7a45;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

:deep(.ant-table-thead > tr > th) {
  background: #f8fafc;
  font-weight: 600;
  color: #475569;
}

:deep(.ant-table-row) {
  cursor: pointer;
}

:deep(.ant-descriptions-title) {
  font-size: 14px;
  color: #64748b;
}
</style>
