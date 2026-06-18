# 质检检测前端联调说明

## 调用链路

前端只调用 Java 主后端，不直接调用 Python、ONNX 或任何模型服务。

当前前端只调用 Java 主后端：

- 图片离线检测：`POST /api/qc-records/detect-image`
- 单帧视频检测：`POST /api/qc-records/detect-frame`
- 实时视频检测：先 `POST /api/qc-stream-sessions` 创建会话，再连接 `WebSocket /ws/qc-stream/{sessionId}`

Java 主后端负责图片保存、ONNX Runtime 本地推理、质检记录落库和附件证据落库。

## 请求字段

- `file`：待检测图片文件。
- `planStepId`：工序计划 ID。
- `qcItemId`：质检项 ID。
- `cameraId`：摄像头 ID，视频帧检测时建议填写。
- `inspectType`：`offline` 或 `video`。
- `inspector`：检验人。
- `remark`：业务备注。

## 返回字段展示

- `sourceImageUrl`：原始上传图相对路径，页面展示为“原图”。
- `imageUrl`：模型输出结果图相对路径，页面展示为“结果图”。
- `resultJudge`：判定结果，取值 `PASS`、`FAIL`、`RECHECK`。
- `confidenceScore`：识别置信度。
- `defectType`：缺陷类型。
- `resultValue`：结果值或算法摘要。
- `boxes`：检测框列表，存在时结构化展示。

## 图片路径规则

Java 返回的图片字段应使用工作区相对路径，例如：

- `photo/upload/20260523/source_001.jpg`
- `photo/results/20260523/result_001.jpg`

前端会把相对路径转换成当前 Java API 基础路径下的可访问 URL。页面只展示相对路径，不写死 Windows 绝对路径。

## 附件证据

结果详情页会通过 `GET /api/inspection-data` 查询附件，并按路径和 `fileType` 区分原图与结果图：

- 包含 `photo/upload/` 的附件显示为原图。
- 包含 `photo/results/` 或 `fileType` 含 `result` 的附件显示为结果图。
