# 织慧通排产系统 v1（开发中）

## 1. 技术方案（v1）
- 后端：FastAPI + SQLAlchemy
- 数据库：MySQL（固定连接，单元测试可用 in-memory SQLite）
- 模式：纯后端 REST API（JSON）
- 架构：单体分层（api / service / model / schema / solver）
- 前端：独立项目，通过 `/api/v1` 对接（详见 `开发过程/前端对接_API清单.md`）

## 2. 数据库连接配置
- host: 127.0.0.1
- port: 3306
- user: root
- password: 通过 `.env` 中的 `DB_PASSWORD` / `db_password` 或系统环境变量配置
- db: zhihuitong_schedule_v1

配置文件：`app/core/config.py`

## 3. 当前已完成
- 项目骨架初始化与数据库初始化脚本（自动建库 + 建表）
- 黑盒机台模型模块（机台、能力参数、班次、快照、日志）
- 约束配置模块（规则 CRUD + 规则项 + 版本保存 / 发布 + 健康度检查）
- 订单管理模块（CRUD + CSV 导入 / 导出 + 模板下载 + 仪表盘聚合）
- 基准排产模块（多策略 solver + 计划版本 + 计划明细 + 未排入任务 + 发布）
- 实时插单重排（freeze_minutes 冻结窗口）与产线补单调度
- 排产甘特图 JSON 数据接口（按机台泳道 + 交期超期标记 + 跨版本变更标记）
- 多策略 KPI benchmark 脚本（greedy_eft / edd / cr / changeover_first / composite 5 策略对比）
- 染色换色规则 v2（固定专用缸 + 循环缸 + 同色族 / 跨色族 / 跨色系三档换色成本 + wash_count）
- LLM 排产解释助手（DeepSeek 接入，离线缓存 + 规则 fallback，仅做解释 / 总结，**不参与排产决策**）
- 阶段 A/B：纯后端化重构（移除 Jinja2 模板 / 静态资源 / pages.py，新增 17 个 REST 端点，输出 1149 行前端对接文档）

## 4. 已建表（全部 v1 表已补齐）
- machine
- machine_capability_param
- machine_shift_availability
- machine_capability_snapshot
- schedule_run_log
- constraint_rule
- constraint_rule_item
- constraint_version
- schedule_task
- schedule_plan_version
- schedule_plan_item
- schedule_unassigned_task

## 5. 初始化与运行
1. 安装依赖
```bash
python -m pip install -r requirements.txt
```

2. 初始化数据库
```bash
python scripts/init_db.py
```

3. 启动服务
```bash
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

4. 访问 API
- 接口清单速览（自动枚举所有路由）：`http://127.0.0.1:8000/`
- 健康检查：`http://127.0.0.1:8000/health`
- Swagger UI：`http://127.0.0.1:8000/docs`
- OpenAPI JSON：`http://127.0.0.1:8000/openapi.json`
- 前端对接文档：`开发过程/前端对接_API清单.md`

## 6. API 模块概览（50 个端点，6 个模块）
| Tag | 前缀 | 用途 |
|---|---|---|
| `order-management` | `/api/v1/orders` | 订单 CRUD + CSV 导入导出 + 仪表盘 + 一键清空 |
| `baseline-schedule` | `/api/v1/schedules` | 基线 / 实时 / 补单排产、计划查询、甘特图 JSON、计划对比、软约束警告 |
| `blackbox-machine` | `/api/v1/machines` | 机台 CRUD、能力参数、班次、快照、一键重置 |
| `constraint-config` | `/api/v1/constraints` | 约束规则 CRUD、版本保存 / 发布、运行时规则、健康度检查 |
| `ai-assistant` | `/api/v1/ai` | LLM 解释未排订单、LLM 总结 benchmark（仅解释层） |
| `timeline-replay` | `/api/v1/timeline` | 时间线回放 CSV / Summary 读取 |

## 7. 黑盒机台模块业务规则（已实现）
- 非 `active` 机台不进候选集
- 机台必须存在启用能力参数
- 机台必须存在"当天可用班次"
- 快照号不可重复
- 快照失败时写日志并返回明确失败原因

## 8. 染色换色规则 v2（已实现）
- **固定专用缸**：黑缸 / 白缸 / 藏蓝缸 / 大红缸**只染专属颜色**，硬约束阻拦非专属颜色
- **循环缸**：通用染缸，遵循深 → 浅 → 深色序约束
- **三档换色成本**：同色族 / 跨色族 / 跨色系（含 undertone 维度，例如青光黑 vs 红光黑）
- **同色连续上限**：避免单缸连续染同色太多导致色准漂移
- **强制洗缸对**（mandatory_wash_pairs）：特定颜色切换必须洗缸，自动累加 `wash_count` KPI

## 9. LLM 排产解释助手（已实现）
- 默认接入 DeepSeek（兼容 OpenAI API 协议），可通过 `.env` 切换
- 内置**离线缓存**与**规则 fallback**，无网时仍可演示
- 严格定位为"解释层 / 辅助决策层"，**不写入** SchedulePlan、**不进** solver 主循环
- 两个端点：`POST /api/v1/ai/explain-unassigned`、`POST /api/v1/ai/summarize-benchmark`

## 10. 单元测试
执行：
```bash
python -m unittest discover -s 测试 -v
```
共 **35 个测试**，覆盖：
- solver KPI 回归（`test_solver_kpi.py`）
- 染色规则与三档换色（`test_color_rules.py`）
- 实时重排与补单（`test_realtime_reschedule.py` / `test_line_compensation.py`）
- LLM 客户端 + 解释 / 总结服务（`test_ai_assistant.py`）
- 时间线回放工具（`test_timeline_service.py`）
- 中文机台名展示（`test_machine_alias_display.py`）
- API 接口（`test_api_order.py` / `test_api_schedule_extra.py` / `test_api_misc.py`）

## 11. 一键模拟排产
执行：
```bash
python scripts/run_demo_simulation.py
```

脚本会自动写入一组演示数据并跑一轮基准排产。

如需更完整的演示数据（含 BASE / RT / COMP 三组、染色固定缸 + 循环缸、13 种颜色），执行：
```bash
python scripts/reset_and_seed_live_demo.py
```

## 12. 多策略 KPI 对比
执行：
```bash
python scripts/benchmark_strategies.py
```

会跑 5 种策略（greedy_eft / edd / cr / changeover_first / composite），生成报告：
`开发过程/演示数据/strategy_benchmark_报告.md`

报告底部包含 LLM 自动总结（如已配置 DeepSeek key），无网时回退到规则总结。

## 13. 后续可推进方向
- OR-Tools / CP-SAT 接入，把"色序"建模为 SDST 调度问题求最优
- 多目标优化（交期满足率 vs 换型成本 vs 机台利用率）
- 与外部 ERP / MES 对接
- 前端独立项目持续完善（甘特图交互、AI 解释面板等）
