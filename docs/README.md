# 织慧通开发文档

这套文档面向新加入项目的开发人员，目标是让对方在半天内理解项目边界、能在本地跑起来，并知道去哪里改业务代码。

## 推荐阅读顺序

1. [01-项目总览.md](01-项目总览.md)
2. [02-快速启动.md](02-快速启动.md)
3. [03-系统架构.md](03-系统架构.md)
4. [04-核心业务流程.md](04-核心业务流程.md)
5. [05-前端开发指南.md](05-前端开发指南.md)
6. [06-后端开发指南.md](06-后端开发指南.md)
7. [07-接口说明.md](07-接口说明.md)
8. [08-数据库说明.md](08-数据库说明.md)
9. [09-认证权限与安全.md](09-认证权限与安全.md)
10. [10-AI能力说明.md](10-AI能力说明.md)
11. [11-测试与排错.md](11-测试与排错.md)
12. [12-部署运维.md](12-部署运维.md)
13. [13-前端ONNX推理完整实施方案.md](13-前端ONNX推理完整实施方案.md)
14. [浏览器ONNX推理实施记录](browser-onnx-implementation/README.md)

## 文档目录约定

```text
docs/
├── README.md
├── 01-项目总览.md
├── 02-快速启动.md
├── ...
├── 12-部署运维.md
├── 13-前端ONNX推理完整实施方案.md
├── browser-onnx-implementation/ # 当前实现、验收结果和数据补丁
├── database/                 # SQL 初始化与迁移脚本
│   ├── zhihuitong.sql
│   ├── auth_schema.sql
│   ├── data_scope_migration.sql
│   ├── ai_decision_support_migration.sql
│   └── 20260830_device_capability_menu.sql
└── best.onnx                 # 质检模型运行资产，后端默认会读取它
```

## 清理说明

已将原来重复分散的文档合并为标准开发文档，并把 SQL 脚本归档到 `docs/database/`。`best.onnx` 是运行资产，不是普通文档，但当前后端默认配置依赖该路径，所以保留在 `docs/` 根目录。
