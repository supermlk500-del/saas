# API 接口

更新时间：2026-06-20

本文列出当前 Controller 对外接口。前端开发代理前缀为 `/prod-api`，表中路径为后端真实路径。

## 1. 通用约定

- 普通接口：`Authorization: Bearer <token>`。
- `/photo/**` 与 `/ws/**` 可使用 `access_token` query 参数。
- 分页参数：`pageNum` 默认 1，`pageSize` 默认 10、最大 200。
- ID 为 Java `Long`，JSON 中按字符串使用。
- 时间格式：`yyyy-MM-dd HH:mm:ss`。

## 2. 认证与系统管理

### 认证 `/auth`

| Method | Path | 鉴权 | 说明 |
| --- | --- | --- | --- |
| GET | `/auth/captcha` | 否 | 返回验证码开关、UUID 和 Base64 PNG |
| POST | `/auth/login` | 否 | 用户名、密码、验证码登录 |
| GET | `/auth/me` | 是 | 当前用户、角色、权限和菜单 |
| PUT | `/auth/password` | 是 | 修改本人密码并使全部会话失效 |
| POST | `/auth/logout` | 是 | 删除当前 Redis 会话 |

登录 Body：

```json
{"username":"admin","password":"***","captchaUuid":"uuid","captchaCode":"ABCD"}
```

### 用户 `/system/user`

| Method | Path | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/system/user/list` | `system:user:list` | 分页及数据范围查询 |
| GET | `/system/user/{userId}` | `system:user:list` | 用户详情，不返回密码 |
| POST | `/system/user` | `system:user:manage` | 管理员创建单角色账号 |
| PUT | `/system/user` | `system:user:manage` | 修改账号资料、部门、岗位、角色 |
| PUT | `/system/user/changeStatus` | `system:user:manage` | 启停账号并强制下线 |
| PUT | `/system/user/resetPwd` | `system:user:manage` | 重置密码并强制下线 |
| DELETE | `/system/user/{userId}` | `system:user:manage` | 逻辑删除并强制下线 |

### 角色与资源

| Method | Path | 权限 |
| --- | --- | --- |
| GET | `/system/role/list` | `system:role:list` |
| GET | `/system/role/{roleId}` | `system:role:list` |
| POST / PUT / DELETE | `/system/role`、`/system/role/{roleId}` | `system:role:manage` |
| PUT | `/system/role/changeStatus` | `system:role:manage` |
| GET | `/system/menu/list` | `system:menu:list` |
| POST / PUT / DELETE | `/system/menu`、`/system/menu/{menuId}` | `system:menu:manage` |
| GET | `/system/dept/list` | `system:dept:list` |
| POST / PUT / DELETE | `/system/dept`、`/system/dept/{deptId}` | `system:dept:manage` |
| GET | `/system/post/list` | `system:post:list` |
| POST / PUT / DELETE | `/system/post`、`/system/post/{postId}` | `system:post:manage` |
| GET | `/system/online` | `system:online:list` |
| DELETE | `/system/online/{sessionId}` | `system:online:forceLogout` |

## 3. 看板

| Method | Path | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/dashboard/overview` | `dashboard:view` | 所有角色可用，按数据范围聚合批次、质检、异常和计划 |

该接口只提供看板读取，不授予业务模块的直接查询或写权限。

## 4. 订单

基础路径：`/api/orders`，读取权限 `order:order:list`。

| Method | Path | 权限/说明 |
| --- | --- | --- |
| GET | `/api/orders` | 列表，支持分页、编号、客户、状态等筛选 |
| GET | `/api/orders/{orderId}` | 聚合详情 |
| POST | `/api/orders` | `order:order:add` |
| PUT | `/api/orders/{orderId}` | `order:order:edit` |
| PATCH | `/api/orders/{orderId}/status` | `order:order:edit` |
| DELETE | `/api/orders/{orderId}` | `order:order:remove` |
| GET/POST | `/api/orders/{orderId}/items` | 明细查询 / `order:order:add` |
| PUT/DELETE | `/api/orders/{orderId}/items/{orderItemId}` | `edit` / `remove` |
| GET/POST | `/api/orders/{orderId}/batches` | 批次分配查询 / `add` |
| PUT/DELETE | `/api/orders/{orderId}/batches/{linkId}` | `edit` / `remove` |
| GET | `/api/orders/{orderId}/plans` | 计划摘要 |
| GET | `/api/orders/{orderId}/routes` | 路线摘要 |
| GET | `/api/orders/{orderId}/machines` | 设备摘要 |
| GET | `/api/orders/{orderId}/qc-records` | 质检摘要 |
| GET | `/api/orders/{orderId}/exceptions` | 异常摘要 |
| GET | `/api/order-schedule-pool` | `plan:production:list`，订单排产池 |

## 5. 来料批次

基础路径：`/api/batches`，读取权限 `batch:resource:list`。

| Method | Path | 权限/说明 |
| --- | --- | --- |
| GET | `/api/batches` | 批次列表 |
| GET | `/api/batches/resource-pool` | 批次资源池 |
| GET | `/api/batches/{batchId}` | 批次详情 |
| POST | `/api/batches` | `batch:resource:add` |
| PUT | `/api/batches/{batchId}` | `batch:resource:edit` |
| DELETE | `/api/batches/{batchId}` | `batch:resource:remove` |
| POST | `/api/batches/import` | `batch:resource:add`，multipart `file` |
| GET | `/api/batches/export` | 导出文件 |

## 6. 工艺与设备

### 工艺路线 `/api/process-routes`

读取权限 `process:route:list`，写权限 `process:route:edit`。

| Method | Path |
| --- | --- |
| GET / POST | `/api/process-routes` |
| GET / PUT / DELETE | `/api/process-routes/{routeId}` |
| GET / POST | `/api/process-routes/{routeId}/steps` |
| PUT / DELETE | `/api/process-routes/{routeId}/steps/{routeStepId}` |

### 工序模板 `/api/process-steps`

| Method | Path |
| --- | --- |
| GET / POST | `/api/process-steps` |
| GET / PUT / DELETE | `/api/process-steps/{stepId}` |
| PATCH | `/api/process-steps/{stepId}/status` |

### 设备 `/api/machines`

读取权限 `process:machine:list`，写权限 `process:machine:edit`。

| Method | Path |
| --- | --- |
| GET / POST | `/api/machines` |
| GET / PUT | `/api/machines/{machineId}` |
| PATCH | `/api/machines/{machineId}/status` |
| GET / POST | `/api/step-machine-capabilities` |
| PUT / DELETE | `/api/step-machine-capabilities/{capId}` |

## 7. 排产

### 生产计划 `/api/production-plans`

| Method | Path | 权限 |
| --- | --- | --- |
| GET | `/api/production-plans` | `plan:production:list` |
| GET | `/api/production-plans/{planId}` | `plan:production:list` |
| POST | `/api/production-plans` | `plan:production:create` |
| PUT | `/api/production-plans/{planId}` | `plan:production:edit` |
| PATCH | `/api/production-plans/{planId}/status` | `plan:production:edit` |
| POST | `/api/production-plans/{planId}/reschedule` | `plan:production:reschedule` |
| GET | `/api/production-plans/{planId}/gantt` | `plan:production:list` |

### 工序计划 `/api/plan-steps`

| Method | Path | 权限 |
| --- | --- | --- |
| GET | `/api/plan-steps` | `plan:production:list` 或 `quality:realtime:view` |
| GET | `/api/plan-steps/{planStepId}` | 同上 |
| PUT | `/api/plan-steps/{planStepId}` | `plan:production:edit` |
| PATCH | `/api/plan-steps/{planStepId}/machine` | `plan:production:edit` |
| PATCH | `/api/plan-steps/{planStepId}/status` | `plan:production:edit` |

### 工艺参数 `/api/process-parameters`

读取权限 `process:route:list`，写权限 `process:route:edit`。

| Method | Path |
| --- | --- |
| GET / POST | `/api/process-parameters` |
| GET / PUT / DELETE | `/api/process-parameters/{paramId}` |

### AI 智能排产 `/api/ai/schedule`

排产计算由 OR-Tools CP-SAT 完成，DeepSeek 仅解释结构化结果。

| Method | Path | 权限/说明 |
| --- | --- | --- |
| POST | `/api/ai/schedule/plans/{planId}/suggestions` | `plan:production:list`，生成交期、利用率、成本三套建议，不修改数据 |
| POST | `/api/ai/schedule/plans/{planId}/apply` | `plan:production:reschedule`，人工确认后重新计算并落库 |
| GET | `/api/ai/schedule/records?planId=` | `plan:production:list`，查询当前数据范围内的历史方案记录 |
| GET | `/api/ai/schedule/records/{recordId}` | `plan:production:list`，恢复指定历史方案快照 |

应用 Body：

```json
{"recordId":"记录ID","strategy":"DELIVERY","confirmationRemark":"排产员确认"}
```

## 8. 质量与 AI 检测

### 质检基础数据

读取权限 `quality:realtime:view`。

| Method | Path | 写权限 |
| --- | --- | --- |
| GET | `/api/qc-items`、`/api/qc-items/{qcItemId}` | - |
| POST / PUT | `/api/qc-items`、`/api/qc-items/{qcItemId}` | `quality:record:review` |
| PATCH | `/api/qc-items/{qcItemId}/status` | `quality:record:review` |
| GET | `/api/qc-cameras` | - |
| GET | `/api/qc-tasks` | - |

### 质检记录 `/api/qc-records`

| Method | Path | 权限/说明 |
| --- | --- | --- |
| GET | `/api/qc-records` | `quality:realtime:view` |
| GET | `/api/qc-records/{inspectionId}` | `quality:realtime:view` |
| POST | `/api/qc-records` | `quality:realtime:detect`，手工记录 |
| POST | `/api/qc-records/detect-image` | `quality:realtime:detect`，multipart 离线 AI 检测 |
| POST | `/api/qc-records/detect-frame` | `quality:realtime:detect`，multipart 单帧检测 |
| PUT | `/api/qc-records/{inspectionId}` | `quality:realtime:detect` |
| PATCH | `/api/qc-records/{inspectionId}/review` | `quality:record:review` |
| PATCH | `/api/qc-records/{inspectionId}/close` | `quality:realtime:detect` |

### 附件 `/api/inspection-data`

| Method | Path | 权限 |
| --- | --- | --- |
| GET | `/api/inspection-data` | `quality:realtime:view` |
| POST | `/api/inspection-data` | `quality:realtime:detect` |
| DELETE | `/api/inspection-data/{dataId}` | `quality:realtime:detect` |

### 实时会话

| Method | Path | 权限 |
| --- | --- | --- |
| POST | `/api/qc-stream-sessions` | `quality:realtime:detect` |
| POST | `/api/qc-stream-sessions/{sessionId}/snapshot` | `quality:realtime:detect` |
| POST | `/api/qc-stream-sessions/{sessionId}/close` | `quality:realtime:detect` |
| WebSocket | `/ws/qc-stream/{sessionId}` | 有效 token 和会话 |

### AI 质检结果分析

| Method | Path | 权限/说明 |
| --- | --- | --- |
| POST | `/api/ai/quality/records/{inspectionId}/analysis` | `quality:realtime:view`，基于 ONNX 结果、近期记录和工艺参数生成风险与根因辅助报告 |

## 9. 异常闭环

基础路径：`/api/exception-records`，读取权限 `exception:record:list`。

| Method | Path | 权限 |
| --- | --- | --- |
| GET | `/api/exception-records` | `exception:record:list` |
| GET | `/api/exception-records/{exceptionId}` | `exception:record:list` |
| POST | `/api/exception-records` | `exception:record:handle` |
| PUT | `/api/exception-records/{exceptionId}` | `exception:record:handle` |
| PATCH | `/api/exception-records/{exceptionId}/status` | `exception:record:handle` |
| PATCH | `/api/exception-records/{exceptionId}/close` | `exception:record:handle` |
| POST | `/api/exception-records/{exceptionId}/rework` | `exception:record:rework` |

## 10. 静态资源与健康检查

| Method | Path | 说明 |
| --- | --- | --- |
| GET | `/photo/**` | 受保护的原图或结果图，支持 `access_token` |
| GET | `/actuator/health` | MySQL、Redis、磁盘和服务健康状态 |
