# 胚布排产质检系统开发文档（接口与数据表集成版）

## 目录

1. 项目概述
   1.1 系统背景
   1.2 系统目标
   1.3 技术架构

2. 数据库设计
   2.1 数据库概览
   2.2 数据表清单及说明
       - batchInfo（批次信息）
       - processRoute（工艺路线）
       - processStep（工序）
       - routeStep（路线工序关联）
       - machine（设备）
       - stepMachineCapability（工序设备能力）
       - productionPlan（排产计划）
       - planStep（排产工序计划）
       - processParameter（工艺参数）
       - qcItem（质检项目）
       - qcRecord（质检记录）
       - qcCamera（工业摄像机）
       - inspectionData（质检数据）
       - exceptionRecord（异常记录）
   2.3 数据库关系链概览
       - 主链路：batchInfo -> productionPlan -> planStep -> { processParameter, qcRecord, exceptionRecord }
       - 工艺链：processRoute -> routeStep -> processStep
       - 设备链：processStep <-> stepMachineCapability <-> machine
       - 质量链：qcItem -> qcRecord, qcCamera -> inspectionData, qcRecord -> inspectionData

3. 功能模块设计
   3.1 来料管理模块
       - 批次录入、修改、删除
       - 批次查询和状态跟踪
   3.2 工艺中心模块
       - 工艺路线管理
       - 工序模板管理
       - 路线工序关联管理
       - 设备及能力管理
       - 质检项目与工业摄像机管理
   3.3 排产管理模块
       - 生产计划生成
       - 工序计划拆分
       - 设备分配与时间安排
   3.4 生产执行模块
       - 工单管理、报工管理、在制品跟踪
   3.5 质量管理模块
       - 质检任务与结果录入
       - 视频/图片质检采集
       - 异常生成与处理闭环
   3.6 追溯与看板模块
       - 批次、工序、质检追溯
       - 实时看板展示

4. 接口设计
   4.1 批次/订单接口
       - GET /api/batches (批次列表查询)
       - GET /api/batches/{batchId} (批次详情)
       - POST /api/batches (新增批次)
       - PUT /api/batches/{batchId} (修改批次)
       - DELETE /api/batches/{batchId} (删除/作废批次)
       - POST /api/batches/import (CSV 批量导入)
       - GET /api/batches/export (导出批次数据)
   4.2 工艺路线接口
       - GET /api/processRoutes, POST /api/processRoutes, PUT /api/processRoutes/{routeId}, DELETE /api/processRoutes/{routeId}
   4.3 工序及排产接口
       - GET /api/planSteps, POST /api/productionPlan, GET /api/productionPlan/{planId}
   4.4 工艺参数接口
       - GET /api/processParameters, POST /api/processParameters
   4.5 设备及能力接口
       - GET /api/machines, POST /api/machines
       - GET /api/stepMachineCapabilities, POST /api/stepMachineCapabilities
   4.6 质检接口
       - GET /api/qcRecords, POST /api/qcRecords
       - GET /api/inspectionData, POST /api/inspectionData
   4.7 异常接口
       - GET /api/exceptionRecords, POST /api/exceptionRecords

5. 核心业务流程
   5.1 来料入厂流程
   5.2 工艺建模流程
   5.3 排产流程
   5.4 工序执行流程
   5.5 质检与异常处理流程

6. 前端功能规划
   - 订单管理、批次列表与详情
   - 编辑/修改批次信息
   - 排产计划展示及甘特图
   - 工序参数录入与调整
   - 质检任务及数据展示
   - 异常处理与反馈

7. 开发与分层建议
   - 后端分层：Controller, Service, Mapper, Entity, DTO/VO
   - 接口风格统一，分页参数规范化

8. 数据字典与状态说明（待补充）
   - 批次状态、工序状态、质检判定值、异常等级等

9. 测试与验证建议
   - 单元测试、接口测试、集成测试
   - 数据完整性、外键约束验证

10. 后续迭代规划
   - 扩展订单、客户、产品表
   - 补充报表中心、AI 辅助排产
   - 权限管理与操作日志模块

