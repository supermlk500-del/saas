# 织慧通 SaaS

织慧通是一个面向纺织生产场景的管理系统，覆盖订单、来料批次、工艺路线、生产排产、质量检测、异常闭环和系统权限。

## 快速入口

- 开发文档入口：[docs/README.md](docs/README.md)
- 快速启动：[docs/02-快速启动.md](docs/02-快速启动.md)
- 系统架构：[docs/03-系统架构.md](docs/03-系统架构.md)
- 接口说明：[docs/07-接口说明.md](docs/07-接口说明.md)
- 部署运维：[docs/12-部署运维.md](docs/12-部署运维.md)

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 前端 | Vue 3、TypeScript、Vite、Ant Design Vue、Pinia |
| 后端 | Spring Boot 3、Spring Security、MyBatis-Plus、WebSocket |
| 数据 | MySQL、Redis |
| AI | ONNX Runtime、OR-Tools、DeepSeek |

## 目录

```text
backend/      Spring Boot 后端
frontend/     Vue 前端
docs/         项目文档、数据库脚本、ONNX 模型
photo/        本地质检图片目录
```

## 本地启动摘要

1. 导入数据库：

```bash
mysql -uroot -p -e "CREATE DATABASE zhihuitong DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -uroot -p zhihuitong < docs/database/zhihuitong.sql
```

2. 启动 Redis。

3. 启动后端：

```bash
cd backend
mvn spring-boot:run
```

4. 启动前端：

```bash
cd frontend
npm install
npm run dev
```

5. 访问：

```text
http://localhost:5173
```

更完整的环境变量和排错说明见：[docs/02-快速启动.md](docs/02-快速启动.md)。
