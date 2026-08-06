# 质检前端联调说明

## 当前调用链

图片质检和实时质检共用 `BrowserInferenceClient`、一个 Web Worker 和一个 ONNX Runtime Web Session。

```text
摄像头/图片 -> Worker 推理 -> 前端绘制结果 -> 仅保存所需证据 -> 后端校验并落库
```

后端不接收普通视频帧做模型推理，也不再提供旧 WebSocket 帧推理、`detect-image`、`detect-frame` 和 `snapshot` 接口。

## 模型接口

- `GET /api/ai/browser-inference/manifest`
- `GET /api/ai/browser-inference/model?sha256={sha256}`

模型清单返回输入输出、类别、解码方式、文件大小和 SHA-256。前端必须使用清单中的类别顺序和模型 SHA，不得自行硬编码或忽略版本。

## 图片检测保存

`POST /api/qc-records/client-detect-results` 使用 `multipart/form-data`：

- `payload`：JSON Blob，包含业务 ID、模型 SHA、图片尺寸、Provider、耗时和检测框；
- `sourceFile`：原图；
- `resultFile`：前端绘制检测框后的结果图。

后端会校验模型 SHA、类别索引、坐标边界、置信度和图片尺寸，校验通过后保存质检记录及附件。

## 实时检测保存

1. `POST /api/qc-stream-sessions` 创建业务会话。
2. 浏览器 Worker 持续推理，普通帧只在页面显示。
3. 连续缺陷命中后，将原图和结果图放入 `evidenceUploadQueue`。
4. 队列调用 `POST /api/qc-stream-sessions/{sessionId}/client-events`。
5. `eventId` 是一次连续缺陷事件的幂等键，失败重试不会重复创建业务记录。
6. 停止页面时先停止接收新证据并排空队列，再关闭业务会话。

证据队列有并发上限和待处理上限。队列满时会丢弃待上传证据并提示用户，但不会阻塞下一次浏览器推理。

## 性能与排错

- 先看页面的预处理、推理、后处理和帧延迟，再看证据队列状态。
- 若推理卡顿，检查 Worker、WebGPU/WASM 和浏览器主线程；不要先增加后端上传并发。
- 若队列失败，检查后端权限、模型 SHA、图片格式、文件存储目录和数据库连接。
- 生产环境必须满足 HTTPS、COOP、COEP、CORP；`.mjs` 返回 `application/javascript`，`.wasm` 返回 `application/wasm`。
