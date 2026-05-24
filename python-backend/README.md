# 胚布排产质检系统 Python 质检后端

这是一个独立的 FastAPI 质检服务，只负责图片/视频帧质检推理能力。

- Java 是主业务后端，负责认证、权限、业务校验、落库、异常闭环和前端接口聚合
- Python 只负责质检算法推理、结果结构化输出、原图与结果图保存
- Python 不直接写 `zhihuitong` 主业务表，不接管排产逻辑

## 目录结构

```text
python-backend/
  app/
    api/
    core/
    schemas/
    services/
    utils/
    vision/
  models/
  main.py
  requirements.txt
  README.md
```

实际图片落盘目录遵循项目约定：

- 原图目录：`C:\Users\lhr\Desktop\saas\photo\upload`
- 结果图目录：`C:\Users\lhr\Desktop\saas\photo\results`

## 已实现接口

### 1. 健康检查

- `GET /api/health`

响应示例：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "status": "UP",
    "service": "python-inspection-service",
    "modelReady": false,
    "modelBackend": "fallback-heuristic"
  }
}
```

### 2. 图片质检

- `POST /api/inspection/image`
- `Content-Type: multipart/form-data`

表单字段：

- `file`：必填，图片文件
- `planStepId`：必填，工序计划 ID
- `qcItemCode`：可选，质检项编码
- `cameraId`：可选，摄像头 ID
- `sourceType`：可选，来源类型，如 `manual`、`camera`

### 3. 视频帧质检

- `POST /api/inspection/frame`
- `Content-Type: multipart/form-data`

表单字段：

- `file`：必填，视频帧图片
- `planStepId`：必填，工序计划 ID
- `cameraId`：可选，摄像头 ID
- `frameTime`：可选，帧时间，格式建议 `yyyy-MM-dd HH:mm:ss`

第一版直接复用图片推理流程，但返回 `inspectType=video`。

## 输出字段说明

接口统一返回：

```json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
```

质检结果字段尽量贴合 `qcrecord / inspectiondata` 语义：

- `inspectType`：`offline` 或 `video`
- `resultJudge`：`PASS`、`FAIL`、`RECHECK`
- `confidenceScore`：识别置信度
- `defectType`：缺陷类型
- `resultValue`：结果摘要，供 Java 写入 `qcrecord.resultValue`
- `imageUrl`：结果图相对路径，如 `photo/results/20260523/result_xxx.jpg`
- `sourceImageUrl`：原图相对路径，如 `photo/upload/20260523/source_xxx.jpg`
- `boxes`：检测框列表

## 文件落盘规则

- 原图保存到 `C:\Users\lhr\Desktop\saas\photo\upload\YYYYMMDD\`
- 结果图保存到 `C:\Users\lhr\Desktop\saas\photo\results\YYYYMMDD\`
- 原图文件名格式：`source_HHMMSS_<unique>.jpg`
- 结果图文件名格式：`result_HHMMSS_<unique>.jpg`
- 返回给 Java 的字段始终使用相对路径，不返回 Windows 绝对路径

服务同时暴露静态访问路径：

- `/photo/upload/**`
- `/photo/results/**`

## 模型加载策略

当前服务已实现可替换推理适配层：

1. 优先尝试加载 `python-backend/models/best.pt`
2. 如果未找到，再尝试 `frontend/best.pt`
3. 若运行环境未安装 `ultralytics` 或模型加载失败，则自动切换到回退推理适配器

默认模型查找顺序：

1. `C:\Users\lhr\Desktop\saas\python-backend\models\best.pt`
2. `C:\Users\lhr\Desktop\saas\frontend\best.pt`

## 启动方式

### 1. 安装依赖

```bash
cd C:\Users\lhr\Desktop\saas\python-backend
pip install -r requirements.txt
```

如需启用 `best.pt` 的 YOLO 推理，请额外安装：

```bash
pip install ultralytics
```

### 2. 启动服务

```bash
cd C:\Users\lhr\Desktop\saas\python-backend
uvicorn main:app --host 0.0.0.0 --port 8001 --reload
```

启动后可访问：

- 健康检查：[http://127.0.0.1:8001/api/health](http://127.0.0.1:8001/api/health)
- Swagger：[http://127.0.0.1:8001/docs](http://127.0.0.1:8001/docs)

## Java 对接建议

- Java 调 Python 时使用 `multipart/form-data`
- Java 负责校验 `planStepId / cameraId / qcItemCode`
- Java 负责将返回结果写入 `qcrecord`、`inspectiondata`
- Java 负责根据 `resultJudge=FAIL` 决定是否走现有 `exceptionrecord` 闭环
- Java 侧建议增加超时、重试和降级提示
- 建议将 Python 返回的 `sourceImageUrl` 和 `imageUrl` 原样持久化，避免再次拼接错误

## 当前第一版说明

- 已可运行：健康检查、图片质检、视频帧质检、文件保存、结果图输出、统一异常响应
- 已实现：`best.pt` 路径加载逻辑、`ultralytics` 推理适配层、回退启发式推理适配层
- 已回退：当模型不可用时使用启发式检测器，保证接口联调不阻塞
