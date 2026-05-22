<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { SettingOutlined } from '@ant-design/icons-vue'

type Camera = {
  id: string
  name: string
  location: string
  running: boolean
  url: string
}

type CameraParamForm = {
  streamId: string
  exposure: number
  frameRate: number
  roi: string
  qualityScore: number
  heartbeat: number
}

const pictureUrls = [
  '/images/quality/qc-1.png',
  '/images/quality/qc-2.png',
  '/images/quality/qc-3.png',
  '/images/quality/qc-4.png',
  '/images/quality/qc-5.png',
  '/images/quality/qc-6.png',
  '/images/quality/qc-7.png',
]

const pickPicture = (index: number) =>
  pictureUrls[index % pictureUrls.length] ?? pictureUrls[0] ?? ''

const cameras = ref<Camera[]>([
  { id: 'c1', name: '车间 相机1-1', location: '验布车间', running: true, url: pickPicture(0) },
  { id: 'c2', name: '车间 相机1-2', location: '验布车间', running: true, url: pickPicture(1) },
  { id: 'c3', name: '车间 相机1-3', location: '验布车间', running: true, url: pickPicture(2) },
  { id: 'c4', name: '车间 相机1-4', location: '验布车间', running: true, url: pickPicture(3) },
  { id: 'c5', name: '车间 相机1-5', location: '验布车间', running: true, url: pickPicture(4) },
  { id: 'c6', name: '车间 相机1-6', location: '验布车间', running: true, url: pickPicture(5) },
])

const createCameraParams = (index: number): CameraParamForm => ({
  streamId: `CAM-WORKSHOP-1-${index + 1}`,
  exposure: 42 + index * 2,
  frameRate: 25,
  roi: 'X: 120  Y: 80  W: 640  H: 420',
  qualityScore: 75,
  heartbeat: 5,
})

const cameraParams = reactive<Record<string, CameraParamForm>>(
  Object.fromEntries(cameras.value.map((camera, index) => [camera.id, createCameraParams(index)])),
)

const paramFor = (cameraId: string) => {
  cameraParams[cameraId] ??= createCameraParams(0)
  return cameraParams[cameraId]!
}

const overview = reactive({
  onlineCameras: 6,
  totalProcessed: 5887.6,
  types: [
    { name: '破洞', count: 5, total: 3 },
    { name: '油污', count: 3, total: 3 },
    { name: '断经', count: 2, total: 2 },
  ],
})

const totalDefects = computed(() => overview.types.reduce((sum, x) => sum + x.count, 0))

const filterForm = reactive({
  product: '华东杭衣',
  grade: '涤纶布C级',
  qty: '1000',
  date: '2026-04-07',
  status: '待处理',
})

type UploadImage = { id: string; url: string; selected: boolean }

const uploadImages = ref<UploadImage[]>([
  { id: 'u1', url: pickPicture(0), selected: true },
  { id: 'u2', url: pickPicture(1), selected: true },
  { id: 'u3', url: pickPicture(2), selected: true },
  { id: 'u4', url: pickPicture(3), selected: false },
])

const onAddCamera = () => {
  const id = `c${cameras.value.length + 1}`
  const nextIndex = cameras.value.length
  cameras.value.push({
    id,
    name: `车间 相机1-${nextIndex + 1}`,
    location: '验布车间',
    running: false,
    url: pickPicture(nextIndex),
  })
  cameraParams[id] = createCameraParams(nextIndex)
  message.success('已添加相机（示例）')
}

const onStart = () => {
  cameras.value = cameras.value.map((c) => ({ ...c, running: true }))
  message.success('已开始检测（示例）')
}

const toggleRunning = (id: string) => {
  const idx = cameras.value.findIndex((c) => c.id === id)
  if (idx < 0) return
  const current = cameras.value[idx]
  if (!current) return
  cameras.value[idx] = { ...current, running: !current.running }
}

const removeCamera = (id: string) => {
  cameras.value = cameras.value.filter((c) => c.id !== id)
  message.info('已移除相机（示例）')
}

const uploadSingle = () => message.info('单图上传（示例）')
const uploadBatch = () => message.info('批量上传（示例）')
const saveCameraParams = (name: string) => message.success(`${name} 参数已保存（示例）`)
</script>

<template>
  <div class="rt-page">
    <div class="header">
      <div class="title">
        <a-typography-title :level="2" style="margin: 0">实时质检</a-typography-title>
        <div class="subtitle">基于改进 YOLO v11 算法的胚布表面缺陷快速智能检测</div>
      </div>
      <div class="actions">
        <a-button type="primary" @click="onAddCamera">+ 添加相机</a-button>
        <a-button
          type="primary"
          style="background: #17b36b; border-color: #17b36b"
          @click="onStart"
        >
          开始检测
        </a-button>
      </div>
    </div>

    <div class="grid">
      <div class="left-panel">
        <div class="panel-title">今日检测概览</div>
        <div class="kv">
          <div class="k">总计相机+直播接入：</div>
          <div class="v">{{ overview.onlineCameras }}</div>
        </div>
        <div class="kv">
          <div class="k">总计图片处理：</div>
          <div class="v">{{ overview.totalProcessed }}</div>
        </div>

        <div class="table-head">
          <div>缺陷类型</div>
          <div style="text-align: right">数量</div>
          <div style="text-align: right">总数</div>
        </div>
        <div class="table-row" v-for="t in overview.types" :key="t.name">
          <div class="tname">{{ t.name }}：</div>
          <div class="tval">{{ t.count }}</div>
          <div class="tval">{{ t.total }}</div>
        </div>
        <div class="table-sum">
          <div>总计</div>
          <div style="text-align: right">{{ totalDefects }}</div>
          <div style="text-align: right">{{ totalDefects }}</div>
        </div>
      </div>

      <div class="main-panel">
        <div class="panel-title">实时视频监控</div>
        <div class="cam-grid">
          <div class="cam-card" v-for="c in cameras" :key="c.id">
            <div class="cam-head">
              <div class="cam-name">{{ c.name }}</div>
              <div class="cam-tools">
                <a-popover
                  trigger="click"
                  placement="bottomRight"
                  overlay-class-name="camera-param-popover"
                >
                  <template #content>
                    <div class="param-panel">
                      <div class="param-title">{{ c.name }} 参数设置</div>
                      <a-form layout="vertical" class="param-form">
                        <a-form-item label="摄像头流 ID">
                          <a-input v-model:value="paramFor(c.id).streamId" />
                        </a-form-item>
                        <div class="param-two-col">
                          <a-form-item label="曝光参数">
                            <a-input-number
                              v-model:value="paramFor(c.id).exposure"
                              :min="1"
                              :max="100"
                            />
                          </a-form-item>
                          <a-form-item label="帧率参数">
                            <a-input-number
                              v-model:value="paramFor(c.id).frameRate"
                              :min="1"
                              :max="60"
                            />
                          </a-form-item>
                        </div>
                        <a-form-item label="ROI 配置">
                          <a-input v-model:value="paramFor(c.id).roi" />
                        </a-form-item>
                        <div class="param-two-col">
                          <a-form-item label="图像评分阈值">
                            <a-input-number
                              v-model:value="paramFor(c.id).qualityScore"
                              :min="0"
                              :max="100"
                            />
                          </a-form-item>
                          <a-form-item label="设备心跳（秒）">
                            <a-input-number
                              v-model:value="paramFor(c.id).heartbeat"
                              :min="1"
                              :max="30"
                            />
                          </a-form-item>
                        </div>
                        <div class="param-status">
                          <span>数据要求</span>
                          <strong>ROI 完整率 100%，评分低于 75 禁止静默入推理</strong>
                        </div>
                        <div class="param-actions">
                          <a-button size="small">取消</a-button>
                          <a-button size="small" type="primary" @click="saveCameraParams(c.name)"
                            >保存参数</a-button
                          >
                        </div>
                      </a-form>
                    </div>
                  </template>
                  <a-button size="small" type="text" class="icon-btn" title="参数设置">
                    <template #icon><SettingOutlined /></template>
                  </a-button>
                </a-popover>
                <a-button size="small" type="text" class="icon-btn" @click="toggleRunning(c.id)">
                  {{ c.running ? 'Ⅱ' : '▶' }}
                </a-button>
                <a-button size="small" type="text" class="icon-btn" @click="removeCamera(c.id)"
                  >×</a-button
                >
              </div>
            </div>
            <div class="cam-body">
              <img :src="c.url" alt="camera" />
              <div class="running-indicator" v-if="c.running">LIVE</div>
            </div>
          </div>
        </div>

        <div class="upload-area">
          <div class="upload-head">
            <div class="panel-title" style="margin: 0">离线图片质检</div>
            <div class="upload-actions">
              <a-button type="primary" @click="uploadSingle">单图上传</a-button>
              <a-button type="primary" @click="uploadBatch">批量上传</a-button>
            </div>
          </div>

          <div class="filters">
            <a-space>
              <a-typography-text class="filter-label">客户</a-typography-text>
              <a-input v-model:value="filterForm.product" style="width: 160px" />
              <a-typography-text class="filter-label">等级</a-typography-text>
              <a-input v-model:value="filterForm.grade" style="width: 160px" />
              <a-typography-text class="filter-label">数量</a-typography-text>
              <a-input v-model:value="filterForm.qty" style="width: 120px" />
              <a-typography-text class="filter-label">日期</a-typography-text>
              <a-input v-model:value="filterForm.date" style="width: 140px" />
              <a-typography-text class="filter-label">状态</a-typography-text>
              <a-input v-model:value="filterForm.status" style="width: 120px" />
            </a-space>
          </div>

          <div class="dropzone">
            <div class="drop-icon">☁</div>
            <div class="drop-text">拖拽图片或点击上传胚布质检图片</div>
          </div>

          <div class="thumbs">
            <div
              class="thumb"
              v-for="img in uploadImages"
              :key="img.id"
              :class="{ selected: img.selected }"
              @click="img.selected = !img.selected"
            >
              <img :src="img.url" alt="upload" />
              <div class="check" v-if="img.selected">✓</div>
              <div class="label">待检测</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.rt-page {
  padding: 0;
  color: #0f172a;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10px;
}

.subtitle {
  margin-top: 6px;
  color: #64748b;
}

.actions {
  display: flex;
  gap: 8px;
}

.grid {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 12px;
}

.left-panel {
  border: 1px solid #e5e7eb;
  background: #ffffff;
  border-radius: 8px;
  padding: 12px;
}

.main-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.panel-title {
  font-size: 16px;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 10px;
}

.kv {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  color: #475569;
}

.table-head,
.table-row,
.table-sum {
  display: grid;
  grid-template-columns: 1fr 70px 70px;
  gap: 8px;
  padding: 8px 0;
}

.table-head {
  margin-top: 8px;
  color: #64748b;
  border-top: 1px solid #e5e7eb;
  border-bottom: 1px solid #e5e7eb;
}

.table-row {
  color: #0f172a;
  border-bottom: 1px solid #eef2f7;
}

.tval {
  text-align: right;
}

.table-sum {
  font-weight: 800;
  color: #0f172a;
  padding-top: 10px;
}

.cam-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.cam-card {
  border: 1px solid #e5e7eb;
  background: #ffffff;
  border-radius: 8px;
  overflow: hidden;
}

.cam-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 10px;
  border-bottom: 1px solid #e5e7eb;
  background: #f8fafc;
  color: #0f172a;
}

.cam-name {
  font-weight: 700;
}

.cam-tools {
  display: flex;
  gap: 2px;
}

.icon-btn {
  color: rgba(15, 23, 42, 0.8);
  width: 28px;
  height: 28px;
  padding: 0;
}

.cam-body {
  position: relative;
  height: 160px;
  background: #0b1220;
}

.cam-body img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0.95;
}

.running-indicator {
  position: absolute;
  top: 10px;
  left: 10px;
  background: rgba(23, 179, 107, 0.9);
  color: #ffffff;
  font-weight: 800;
  border-radius: 6px;
  padding: 2px 8px;
  font-size: 12px;
}

.upload-area {
  border: 1px solid #e5e7eb;
  background: #ffffff;
  border-radius: 8px;
  padding: 12px;
}

.upload-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.upload-actions {
  display: flex;
  gap: 8px;
}

.filters {
  color: #475569;
  margin-bottom: 10px;
}

.dropzone {
  border: 2px dashed #cbd5e1;
  border-radius: 8px;
  background: #f8fafc;
  padding: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 10px;
}

.drop-icon {
  font-size: 18px;
  color: #1677ff;
}

.drop-text {
  color: #334155;
  font-weight: 700;
}

.thumbs {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 10px;
}

.thumb {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  cursor: pointer;
  background: #ffffff;
}

.thumb.selected {
  border-color: #1677ff;
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.18);
}

.thumb img {
  width: 100%;
  height: 84px;
  object-fit: cover;
  opacity: 1;
}

.check {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 18px;
  height: 18px;
  border-radius: 999px;
  background: rgba(226, 232, 240, 0.9);
  color: rgba(15, 23, 42, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  font-size: 12px;
}

.label {
  position: absolute;
  bottom: 6px;
  left: 6px;
  background: rgba(15, 23, 42, 0.6);
  color: rgba(255, 255, 255, 0.9);
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
}

.filter-label {
  color: #475569;
}

:global(.camera-param-popover) {
  max-width: 360px;
}

:global(.camera-param-popover .ant-popover-inner) {
  border-radius: 8px;
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.16);
}

.param-panel {
  width: 320px;
}

.param-title {
  color: #0f172a;
  font-size: 15px;
  font-weight: 800;
  margin-bottom: 10px;
}

.param-form :deep(.ant-form-item) {
  margin-bottom: 10px;
}

.param-form :deep(.ant-form-item-label) {
  padding-bottom: 3px;
}

.param-form :deep(.ant-form-item-label > label) {
  color: #475569;
  font-size: 12px;
  font-weight: 700;
}

.param-form :deep(.ant-input-number) {
  width: 100%;
}

.param-two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.param-status {
  border: 1px solid #dbeafe;
  background: #eff6ff;
  border-radius: 8px;
  padding: 8px 10px;
  color: #1e3a8a;
  font-size: 12px;
  line-height: 1.5;
}

.param-status span {
  display: block;
  color: #64748b;
  font-weight: 700;
  margin-bottom: 2px;
}

.param-status strong {
  font-weight: 800;
}

.param-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}
</style>
