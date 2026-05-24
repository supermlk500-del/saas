## 2026-05-23 09:20

# 胚布排产质检系统 Java 后端开发文档

## 1. 文档目标

本文档用于明确当前系统中 Java 后端的最终职责、架构定位与后续开发重点。

当前最终决策如下：

- 排产算法模块继续留在 Java 后端实现
- 质检算法模块不再使用独立 Python 后端
- 质检推理能力改为 Java 后端内部集成 ONNX Runtime 本地推理

因此，Java 后端后续重点有两部分：

1. 持续完善排产算法与排产业务
2. 在 Java 后端内部集成 ONNX 模型推理，完成质检检测、结果落库与异常闭环

## 2. 当前系统现状

项目根目录：

- `C:\Users\lhr\Desktop\saas`

Java 后端目录：

- `C:\Users\lhr\Desktop\saas\backend`

当前技术栈：

- `Spring Boot`
- `MyBatis-Plus`
- `MySQL`

当前已实现模块：

- `batch`
- `process`
- `plan`
- `quality`
- `exception`

当前状态判断：

- 排产主链路已经在 Java 中真实存在，不是空壳
- `ProductionPlanService` 已具备计划创建、计划重排、工序展开、设备推荐等基础能力
- `PlanStepService` 已具备设备调整、状态流转、计划时间调整等能力
- 质检模块已经具备 `qc-records`、`inspection-data`、异常自动创建能力
- 实时质检前端尚未真正联动本地模型推理结果

质检图片存储约定：

- 原始上传图片统一落盘到 `C:\Users\lhr\Desktop\saas\photo\upload`
- 算法结果图统一落盘到 `C:\Users\lhr\Desktop\saas\photo\results`
- 业务表中建议保存 `photo/...` 相对路径，而不是保存绝对路径

结论：

- Java 后端继续作为唯一主业务后端
- 排产算法不迁移语言
- 质检检测链路改为 Java 内部调用 ONNX Runtime

## 3. Java 后端定位

Java 后端继续承担以下职责：

- 业务主流程控制
- 用户与权限
- 前端 REST 接口
- 数据落库
- 状态流转
- 异常闭环
- 排产算法执行
- ONNX 模型推理集成

## 4. 总体架构

最终架构建议如下：

`Frontend -> Spring Boot -> ONNX Runtime -> MySQL`

更细分的职责为：

- 前端：发起计划、质检、追溯等业务请求
- Java Controller：提供统一业务接口
- Java Service：组织业务流程、调用排产逻辑、调用 ONNX 推理、完成落库
- Java AI 模块：加载 `.onnx` 模型、执行图片推理、输出结构化检测结果
- MySQL：存储计划、质检记录、附件、异常等业务数据

## 5. 排产模块开发建议

### 5.1 原则

排产算法继续在 Java 中做，不新增 Python 排产服务。

建议原则：

1. 保持现有前端接口不变
2. 保持当前数据库结构不大改
3. 优先优化 `ProductionPlanService`
4. 先提升“可用解质量”，再考虑复杂优化算法

### 5.2 推荐演进方向

#### 第一阶段

- 提升当前规则排产质量
- 完善设备推荐策略
- 补充更多业务校验

#### 第二阶段

- 引入更细粒度约束
- 增加计划评分机制
- 增加推荐说明字段

#### 第三阶段

- 若需要，可在 Java 内部引入更复杂算法实现
- 但仍保持主系统和落库逻辑在 Java 中

## 6. 质检 ONNX 推理模块设计

### 6.1 原则

当前系统的正式实施原则如下：

1. 前端业务接口保持稳定
2. 数据库表结构延续当前核心主链路设计
3. 质检检测由 Java 内部 ONNX Runtime 统一承担
4. 排产与质检仍然统一收敛在 Java 主后端中

### 6.2 推荐模块

建议在 Java 后端新增：

```text
com.zhihuitong.modules.ai
```

建议类如下：

- `OnnxYoloService`
- `YoloDetectResult`
- `YoloBox`
- `ImagePreprocessUtils`

职责说明：

- `OnnxYoloService`：模型加载、推理执行、输出解析
- `YoloDetectResult`：封装整张图片的检测结果
- `YoloBox`：封装单个缺陷框
- `ImagePreprocessUtils`：图片缩放、归一化、张量转换等预处理

### 6.3 建议文件结构

```text
backend/src/main/java/com/zhihuitong/modules/
  ai/
    service/
      OnnxYoloService.java
    model/
      YoloDetectResult.java
      YoloBox.java
    util/
      ImagePreprocessUtils.java
```

## 7. 模型与配置建议

### 7.1 模型文件位置

当前已训练好的 ONNX 模型文件位置为：

```text
C:\Users\lhr\Desktop\saas\docs\best.onnx
```

第一阶段开发建议直接基于该模型文件完成联调与推理接入。

后续若需要再做工程化收敛，可考虑以下两种放置方式：

1. 资源目录方式：

```text
backend/src/main/resources/models/fabric_defect.onnx
```

2. 外部目录方式：

```text
backend/models/fabric_defect.onnx
```

第一版更推荐资源目录方式，便于统一打包和部署。

当前阶段补充建议：

- 若先以最小改动接入，可直接读取 `C:\Users\lhr\Desktop\saas\docs\best.onnx`
- 若后续进入部署阶段，再将模型迁移到 `resources/models/` 或 `backend/models/`

### 7.2 配置文件建议

建议在 Java 配置文件中新增：

```yaml
ai:
  yolo:
    enabled: true
    model-path: C:/Users/lhr/Desktop/saas/docs/best.onnx
    conf-threshold: 0.5
    iou-threshold: 0.45
```

### 7.3 Maven 依赖建议

建议新增 ONNX Runtime 依赖：

```xml
<dependency>
    <groupId>com.microsoft.onnxruntime</groupId>
    <artifactId>onnxruntime</artifactId>
    <version>1.18.0</version>
</dependency>
```

说明：

- 当前配置建议优先直接指向现有模型 `C:\Users\lhr\Desktop\saas\docs\best.onnx`
- 初期建议先用 CPU 版本跑通
- GPU 版本后续再单独评估 CUDA 与 `onnxruntime-gpu`

## 8. 质检业务调用链设计

### 8.1 前端接口保持稳定

前端仍应调用 Java，不直接感知模型推理实现变更。

推荐正式入口拆分为两类：

- `POST /api/qc-records/detect-image`
- `POST /api/qc-stream-sessions`

同时建议补充摄像头资源接口：

- `GET /api/qc-cameras`

以及实时流式检测接口：

- `WebSocket /ws/qc-stream/{sessionId}`
- `POST /api/qc-stream-sessions/{sessionId}/snapshot`
- `POST /api/qc-stream-sessions/{sessionId}/close`

也可兼容现有 `qc-records` 录入接口，但建议将“检测触发”和“纯手工录入”分离。

图片离线质检的正式处理顺序建议固定为：

1. 接收 `multipart/form-data`
2. 将原图真实落盘到 `photo/upload/YYYYMMDD/`
3. 调用 ONNX Runtime 本地推理
4. 生成并落盘结果图到 `photo/results/YYYYMMDD/`
5. 写入 `qcrecord`
6. 写入 `inspectiondata`
7. 若结果为 `FAIL`，复用异常自动创建逻辑

实时质检页交互补充说明：

- `/quality/realtime` 页面应拆分为：
  - 图片离线质检
  - 实时视频流质检
- 页面顶部不再展示固定链路提示条
- 实时视频流质检模式下，前端通过浏览器本地能力调用电脑摄像头
- Java 后端不直接控制用户电脑摄像头，而是通过流式通道持续接收视频帧并返回检测结果
- 实时视频流质检不应以“截图检测”作为主路径

### 8.2 后端内部调用链

推荐链路如下：

图片离线质检链路：

1. 前端上传单张图片
2. Java 接收图片文件与业务参数
3. Java 将原图保存到 `photo\upload\YYYYMMDD\`
4. Java 调用 `OnnxYoloService` 进行本地推理
5. Java 生成结果图并保存到 `photo\results\YYYYMMDD\`
6. Java 将检测结果写入 `qcrecord`
7. Java 将原图和结果图路径写入 `inspectiondata`
8. 若 `resultJudge=FAIL`，复用现有异常自动生成逻辑

实时视频流质检链路：

1. 前端创建视频流检测会话
2. 前端调用电脑摄像头，持续采集视频帧
3. 前端通过 WebSocket 向 Java 持续发送视频帧
4. Java 对每一帧或采样帧执行 ONNX 推理
5. Java 将检测框、判定结果、置信度实时返回前端
6. 前端在视频预览层上实时叠加检测框
7. 仅当出现异常帧、关键帧或人工确认保存时，Java 再将该帧写入 `qcrecord` 和 `inspectiondata`

视频源管理补充说明：

- 第一阶段默认使用“电脑摄像头”作为第一类视频源
- 前端通过浏览器选择本机摄像头设备
- 后端通过 `qccamera` 维护摄像头业务主数据
- 后续可平滑扩展工业摄像头、网络摄像头等其他来源

### 8.3 文件流转约定

- 原始图片物理位置：`C:\Users\lhr\Desktop\saas\photo\upload\YYYYMMDD\`
- 结果图物理位置：`C:\Users\lhr\Desktop\saas\photo\results\YYYYMMDD\`
- 原图文件名建议规则：`source_yyyyMMddHHmmssSSS_8位随机串.jpg`
- 结果图文件名建议规则：`result_yyyyMMddHHmmssSSS_8位随机串.jpg`
- 落库字段建议保存：
  - `photo/upload/20260523/source_001.jpg`
  - `photo/results/20260523/result_001.jpg`

## 9. 质检接口实现建议

### 9.1 推荐检测接口

接口名称：图片离线质检

- 请求方式：`POST`
- 请求路径：`/api/qc-records/detect-image`
- 请求类型：`multipart/form-data`
- `inspectType` 固定语义：`offline`

建议请求参数：

- `file`：图片文件，必填
- `planStepId`：工序计划 ID，必填
- `qcItemId`：质检项 ID，必填
- `cameraId`：摄像头 ID，可为空
- `inspector`：检验人，可选
- `remark`：备注，可选

接口名称：实时视频流质检会话创建

- 请求方式：`POST`
- 请求路径：`/api/qc-stream-sessions`
- 请求类型：`application/json`

建议请求参数：

- `planStepId`：工序计划 ID，必填
- `qcItemId`：质检项 ID，必填
- `cameraId`：摄像头 ID，必填
- `inspector`：检验人，可选
- `remark`：备注，可选

建议响应内容：

- `sessionId`
- `streamMode`
- `cameraId`
- `startedAt`

接口名称：实时视频流传输

- 请求方式：`WebSocket`
- 请求路径：`/ws/qc-stream/{sessionId}`
- `inspectType` 固定语义：`video`

建议返回内容：

- `sessionId`
- `frameTime`
- `resultJudge`
- `confidenceScore`
- `resultValue`
- `boxes`
- `renderMode`

接口名称：实时视频流关键帧保存

- 请求方式：`POST`
- 请求路径：`/api/qc-stream-sessions/{sessionId}/snapshot`
- 请求类型：`multipart/form-data`

建议请求参数：

- `file`：当前关键帧图片，必填
- `frameTime`：帧时间，可选
- `resultJudge`：当前帧判定结果，可选
- `confidenceScore`：当前帧识别置信度，可选
- `resultValue`：当前帧结果值，可选
- `remark`：备注，可选

离线检测与关键帧保存统一建议响应内容：

- `inspectionId`
- `resultJudge`
- `confidenceScore`
- `resultValue`
- `imageUrl`
- `sourceImageUrl`
- `boxes`

补充摄像头资源接口：

- `GET /api/qc-cameras`
- 第一阶段建议由后端自动初始化一条默认记录：
  - `cameraCode=LOCAL_CAM_001`
  - `cameraName=电脑摄像头`
  - `cameraType=local_webcam`
  - `location=质检工作站本机`

### 9.2 纯手工录入接口

现有 `POST /api/qc-records` 可继续保留，作为非算法场景下的人工录入接口。

## 10. 数据落库建议

### 10.1 qcrecord

建议由 Java 根据 ONNX 推理结果写入：

- `planStepId`
- `qcItemId`
- `inspectTime`
- `inspectType`
- `cameraId`
- `frameTime`
- `imageUrl`
- `confidenceScore`
- `resultValue`
- `resultJudge`
- `inspector`
- `remark`

其中：

- `imageUrl` 建议保存结果图相对路径，如 `photo/results/20260523/result_001.jpg`

### 10.2 inspectiondata

建议写入：

- `qcRecordId`
- `cameraId`
- `fileType`
- `filePath`
- `fileName`
- `captureTime`
- `resultSummary`
- `remark`

其中：

- 原始上传图建议 `filePath=photo/upload/20260523/source_001.jpg`
- 若需要记录结果图，也可增加对应附件记录，或在 `resultSummary/remark` 中补充说明

### 10.3 exceptionrecord

继续复用现有逻辑：

- 当质检结果为 `FAIL`
- 若未存在对应未关闭质量异常
- 则自动生成 `QUALITY` 异常记录

## 11. 当前实施顺序

当前系统建议继续沿以下顺序迭代：

1. 保持 Java 内部 ONNX Runtime 本地推理链路稳定
2. 持续优化图片离线质检与实时视频流质检效果
3. 完善质检记录、附件留档、异常闭环联动
4. 继续增强排产主链路与订单扩展能力

## 12. 异常处理建议

Java 调用本地 ONNX 推理时，必须考虑：

- 模型文件不存在
- 模型加载失败
- 输入图片格式异常
- 推理结果为空
- 结果图输出失败
- 文件写入失败

建议处理策略：

1. 返回清晰业务错误信息
2. 记录推理与文件处理日志
3. 不影响已有人工质检录入能力
4. 保留人工兜底路径
5. 对 `C:\Users\lhr\Desktop\saas\photo\upload` 与 `C:\Users\lhr\Desktop\saas\photo\results` 的目录可用性进行启动前校验

## 13. 风险与注意事项

### 13.1 排产风险

- 若在已有逻辑上大幅重写，容易影响计划创建、重排、甘特图、设备分配等现有功能

建议：

- 增量优化
- 保持现有接口稳定

### 13.2 推理风险

- Java 侧 ONNX 推理若预处理、类别映射、输出解析处理不当，会导致与原训练结果偏差较大

建议：

- 首先完成单图验证
- 明确类别映射表
- 保留结果可视化检查能力

### 13.3 代码边界风险

- 若 ONNX 推理逻辑散落在多个 Controller/Service 中，后续维护成本高

建议：

- 明确新增独立 `modules.ai` 模块
- 统一封装推理入口

## 14. 结论

当前系统最合理的后端分工应为：

- Java：主业务系统 + 排产算法 + ONNX 本地推理 + 质检结果落库 + 异常闭环
- MySQL：承接业务数据

这条方案最符合你当前代码现状，也最能控制改造成本和实施风险。

---
