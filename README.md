# 织慧通

织慧通是面向纺织生产企业的管理系统，覆盖订单、来料批次、工艺路线、设备能力、生产计划、甘特图、质量检测、异常处理和 AI 辅助分析。

系统主流程：

```text
订单 → 订单明细 → 来料批次 → 工艺路线/工序/设备
→ 设备能力 → 生产计划 → 甘特图 → 质检 → AI 分析/异常处理
```

## 先看结论：需要显卡或 CUDA 吗？

不需要。

- 不需要 NVIDIA 显卡、CUDA、cuDNN 或 NVIDIA Container Toolkit；
- 后端不执行 ONNX 推理，只负责提供模型、校验结果和保存业务数据；
- 浏览器端使用 ONNX Runtime Web 执行质检模型；
- 浏览器优先使用 WebGPU，不支持时自动回退到 WASM；
- WebGPU 使用的是运行演示电脑的 GPU，不是远程服务器的 GPU；
- OR-Tools 排产算法使用 CPU；
- DeepSeek 通过网络 API 调用，不需要本地 GPU。

普通办公电脑即可运行本项目。GPU 只会影响浏览器端模型推理速度，不影响系统启动。

## 环境要求

| 环境 | 要求 | 用途 |
| --- | --- | --- |
| 操作系统 | Windows 10/11、Linux 或 macOS | Windows 推荐 PowerShell |
| JDK | 17 | 运行 Spring Boot 后端 |
| Maven | 3.8+，推荐 3.9+ | 构建和启动后端 |
| Node.js | 20.19+ 或 22.12+ | 运行前端；Docker 构建使用 Node 22 |
| npm | 随 Node.js 安装 | 安装前端依赖 |
| MySQL | 8.x | 业务数据库，使用 utf8mb4 |
| Redis | 6+，推荐 7.x | 登录会话、验证码和在线状态 |
| 浏览器 | 较新的 Chrome 或 Edge | 运行前端和浏览器端质检 |

建议开发机至少准备 4 核 CPU、8GB 内存和 10GB 可用磁盘空间。小数据量运行 2 核、4GB 内存也可以，但安装依赖和 Docker 构建会较慢。

### 浏览器和摄像头

- 图片质检不要求摄像头；
- 实时质检需要浏览器摄像头权限；
- 本机使用 `localhost` 可以进行摄像头测试；
- 远程服务器部署实时质检时建议使用 HTTPS；
- WebGPU 不可用时会回退到 WASM。

## 项目目录

```text
backend/                         Spring Boot 后端
frontend/                        Vue 3 前端
docs/database/                   数据库结构和初始化 SQL
docs/best.onnx                  浏览器端质检模型
photo/upload/                    上传原图目录
photo/results/                   质检结果图目录
deploy/single-container/        单容器环境变量和 Nginx 配置
docker-compose.single.yml       单应用容器 Compose 配置
docker-compose.prod.yml          前端、后端、Redis 多容器配置
```

## 方式一：本地直接启动

适合开发和录制演示视频，需要在电脑上安装 JDK、Maven、Node.js、MySQL 和 Redis。

### 1. 检查模型文件

确认下面文件存在：

```text
docs/best.onnx
```

这是浏览器端质检模型，不能删除。模型文件需要与后端的浏览器推理 Manifest 保持一致，否则会出现模型大小或 SHA-256 校验失败。

### 2. 初始化 MySQL

创建数据库：

```sql
CREATE DATABASE zhihuitong
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

导入初始化脚本：

```bash
mysql -uroot -p zhihuitong < docs/database/zhihuitong.sql
```

全新安装通常直接导入 `zhihuitong.sql` 即可。升级旧库时，再根据版本执行 `docs/database/` 下的迁移脚本。

### 3. 启动 Redis

已经安装 Docker 时，推荐：

```bash
docker run -d --name zhihuitong-redis --restart unless-stopped -p 6379:6379 redis:7-alpine
```

无密码时，`REDIS_PASSWORD` 留空；有密码时，应用和 Redis 必须使用同一个密码。

### 4. 配置后端

将 `.env.example` 复制为项目根目录的 `.env`，并填写本机配置：

```properties
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_DATABASE=zhihuitong
MYSQL_USERNAME=root
MYSQL_PASSWORD=你的MySQL密码

REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

JWT_SECRET=change-this-development-secret-to-at-least-32-bytes
DEEPSEEK_ENABLED=true
DEEPSEEK_API_KEY=你的DeepSeek密钥
DEEPSEEK_MODEL=deepseek-v4-flash

AI_YOLO_ENABLED=true
AI_YOLO_MODEL_PATH=docs/best.onnx
INSPECTION_STORAGE_UPLOAD_ROOT=photo/upload
INSPECTION_STORAGE_RESULT_ROOT=photo/results
```

说明：

- `JWT_SECRET` 至少 32 个字符，否则后端无法启动；
- `DEEPSEEK_API_KEY` 用于排产解释和质检分析，不配置时部分功能会使用规则结果；
- `CAPTCHA_ENABLED` 默认开启，仅本地测试时可以设为 `false`；
- `.env` 已被 Git 忽略，不能提交到仓库。

### 5. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认地址：`http://localhost:8080`。

### 6. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`。前端会把 `/prod-api` 请求代理到后端 `8080` 端口。

## 方式二：单应用容器启动

适合 Jenkins 构建和服务器部署。宿主机不需要安装 JDK、Maven 或 Node.js，Docker 构建阶段会使用 Dockerfile 中的 Maven、JDK 和 Node 镜像。

注意：这是“单应用容器”，容器内包含 Nginx、Spring Boot 和 ONNX 模型；MySQL 和 Redis 仍然是外部服务。

### 1. 准备 Docker

需要安装 Docker Engine 或 Docker Desktop，并支持 Docker Compose V2，即 `docker compose` 命令。首次构建需要访问 Docker Hub、Maven 仓库和 npm registry。

### 2. 创建配置文件

Linux/macOS：

```bash
cp deploy/single-container/env.example deploy/single-container/env
```

Windows PowerShell：

```powershell
Copy-Item deploy\single-container\env.example deploy\single-container\env
```

编辑 `deploy/single-container/env`，至少填写：

```properties
MYSQL_HOST=host.docker.internal
MYSQL_PORT=3306
MYSQL_DATABASE=zhihuitong
MYSQL_USERNAME=你的数据库账号
MYSQL_PASSWORD=你的数据库密码
REDIS_HOST=host.docker.internal
REDIS_PORT=6379
REDIS_PASSWORD=
JWT_SECRET=replace-with-a-random-secret-at-least-32-characters
DEEPSEEK_ENABLED=true
DEEPSEEK_API_KEY=你的DeepSeek密钥
DEEPSEEK_MODEL=deepseek-v4-flash
```

如果 MySQL 或 Redis 在另一台服务器，将 `host.docker.internal` 改成对应的内网 IP 或域名，并确保防火墙允许访问 3306 和 6379。

### 3. 构建并启动

```bash
docker compose -f docker-compose.single.yml up -d --build
```

应用访问地址：`http://localhost:18080`。

检查状态：

```bash
docker ps
docker logs --tail=200 zhihuitong
```

停止应用：

```bash
docker compose -f docker-compose.single.yml down
```

单容器会把本地 `photo/` 挂载到容器的 `/app/data`，上传图片和质检结果不会因为应用容器重建而丢失。

## 方式三：前端、后端、Redis 多容器

`docker-compose.prod.yml` 适合服务器环境：

```text
saas-frontend + saas-backend + saas-redis + 外部 MySQL
```

准备以下目录：

```text
/opt/saas/docs/best.onnx
/opt/saas/data/upload
/opt/saas/data/results
/opt/saas/redis
```

特别注意：`/opt/saas/docs` 会覆盖容器内的 `/app/docs`，所以必须把 `best.onnx` 放进 `/opt/saas/docs`，否则后端会报 ONNX 模型不存在。

配置根目录 `.env` 后启动：

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

## AI 功能依赖

### 浏览器端 ONNX 质检

依赖 `docs/best.onnx`、现代浏览器和可用的后端模型接口。WebGPU 是加速选项，WASM 是 CPU 回退方案。不依赖 CUDA、cuDNN、NVIDIA 显卡、Python 或 PyTorch。

### AI 智能排产

- OR-Tools 负责排产计算和约束求解；
- DeepSeek 负责解释排产方案和生成可读报告；
- DeepSeek Key 未配置时，核心算法仍可运行，但可能只能看到规则结果；
- 排产算法不依赖本地 GPU。

### AI 质检分析

- ONNX 模型负责图片缺陷检测；
- 后端负责校验浏览器提交的检测结果并保存记录；
- DeepSeek 负责生成质量分析和根因辅助说明；
- 没有 Key 时使用规则分析结果，不会生成真实的 DeepSeek 文本。

## 端口说明

| 端口 | 服务 | 本地开发 | 容器部署 |
| --- | --- | --- | --- |
| 5173 | Vite 前端 | 使用 | 不对外开放 |
| 8080 | Spring Boot 后端 | 使用 | 容器内部使用 8081 |
| 18080 | Nginx 应用入口 | 通常不用 | 使用 |
| 3306 | MySQL | 使用 | 外部服务 |
| 6379 | Redis | 使用 | 外部或 Redis 容器 |

## 常见问题

### 后端提示 JWT secret must be at least 32 bytes

检查 `JWT_SECRET`，长度必须至少 32 个字符，然后重启后端。

### 验证码不显示或登录状态失效

检查后端、Redis、Redis 连接配置，以及前端 `/prod-api/auth/captcha` 是否能代理到后端。

### 质检模型加载失败

检查 `docs/best.onnx`、`AI_YOLO_MODEL_PATH`，以及 Docker 多容器部署时的 `/opt/saas/docs/best.onnx`。不要随意替换模型，否则可能与 Manifest 的文件大小和 SHA-256 不一致。

### DeepSeek 没有生成报告

检查 `DEEPSEEK_ENABLED=true`、`DEEPSEEK_API_KEY`、`DEEPSEEK_MODEL=deepseek-v4-flash`，并确认服务器可以访问 `https://api.deepseek.com`。

### 实时质检无法打开摄像头

检查浏览器摄像头权限。远程服务器访问时使用 HTTPS，本机 `localhost` 一般可以直接测试。

## 安全注意事项

以下内容不能提交到 Git：

- MySQL 密码；
- Redis 密码；
- JWT_SECRET；
- DeepSeek API Key；
- 服务器私钥和证书。

如果 API Key 曾经提交到公开仓库，应立即撤销并重新生成。

## 文档入口

- [开发文档目录](docs/README.md)
- [快速启动](docs/02-快速启动.md)
- [系统架构](docs/03-系统架构.md)
- [接口说明](docs/07-接口说明.md)
- [数据库说明](docs/08-数据库说明.md)
- [AI 能力说明](docs/10-AI能力说明.md)
- [部署运维](docs/12-部署运维.md)
- [Jenkins 单容器部署](docs/16-Jenkins单容器部署.md)
