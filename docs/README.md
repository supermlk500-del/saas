# AI胚布排产质检系统开发文档

更新时间：2026-06-20

本目录只保留当前开发、调试和维护需要的资料。代码实现与文档冲突时，以当前代码为准，并同步修正文档。

## 阅读顺序

1. [开发指南](./开发指南.md)：环境、数据库初始化、启动、构建和配置。
2. [系统设计](./系统设计.md)：技术架构、模块职责和核心业务流程。
3. [API 接口](./API接口.md)：当前后端接口、权限与统一响应。
4. [数据库设计](./数据库设计.md)：业务表、权限表及关键关系。
5. [权限与认证](./权限与认证.md)：角色、JWT、Redis、菜单和数据权限。
6. [测试指南](./测试指南.md)：自动化命令与必须执行的回归场景。

## 初始化与运行资源

| 文件 | 用途 |
| --- | --- |
| [`zhihuitong.sql`](./zhihuitong.sql) | 17 张核心业务表及基础业务数据 |
| [`auth_schema.sql`](./auth_schema.sql) | 用户、角色、菜单、部门、岗位和登录日志 |
| [`data_scope_migration.sql`](./data_scope_migration.sql) | 为核心业务表补充 `deptId/createdBy` 数据归属字段 |
| [`ai_decision_support_migration.sql`](./ai_decision_support_migration.sql) | 为现有数据库增量增加 AI 智能排产与 AI 质检分析菜单 |
| [`best.onnx`](./best.onnx) | Java 后端加载的 AI 质检模型 |

SQL 固定执行顺序：`zhihuitong.sql` → `auth_schema.sql` → `data_scope_migration.sql`。

已初始化的数据库升级本功能时，只需额外执行 `ai_decision_support_migration.sql`，不要重新执行会重建权限表的 `auth_schema.sql`。

## 维护规则

- 新增或修改接口：更新 [API接口.md](./API接口.md)。
- 修改表结构：更新 [数据库设计.md](./数据库设计.md) 和对应 SQL。
- 修改角色、菜单或权限字符串：更新 [权限与认证.md](./权限与认证.md)。
- 修改启动命令、环境变量或配置：更新 [开发指南.md](./开发指南.md)。
- 修复关键业务缺陷或新增流程：补充 [测试指南.md](./测试指南.md) 的回归项。
- 不再新增“核对报告”“阶段总结”“临时方案”等独立文档；有效结论直接合并到上述主题文档。
