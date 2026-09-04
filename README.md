# 织慧通

织慧通是一套面向纺织生产企业的智能生产管理系统，围绕订单、来料、工艺、设备、排产和质量建立统一的业务闭环。

## 项目简介

系统用于管理纺织生产过程中的核心业务信息，并通过规则引擎、运筹优化算法和浏览器端视觉模型辅助生产决策。

主要功能包括：

- 账号登录、角色权限和部门数据权限；
- 客户订单、订单明细和批次资源管理；
- 工艺路线、生产工序、设备和设备能力管理；
- 生产计划创建、工序计划和调度甘特图；
- 基于 OR-Tools 的智能排产方案生成；
- 基于浏览器端 ONNX 模型的图片质检和实时质检；
- 质检结果、异常记录、返工处理和质量分析；
- 使用 DeepSeek 对排产结果和质量结果进行解释和报告生成。

核心业务流程：

```text
订单 → 订单明细 → 来料批次 → 工艺路线、工序和设备
→ 设备能力 → 生产计划 → 甘特图和生产执行
→ 图片/实时质检 → AI 分析、异常处理和返工
```

## 系统架构

```text
浏览器
  ├─ Vue 3 前端
  ├─ ONNX Runtime Web（WebGPU/WASM）执行质检推理
  └─ 通过 /prod-api 访问后端

Spring Boot 后端
  ├─ 订单、批次、工艺、设备、排产和质检业务
  ├─ OR-Tools 计算排产方案
  ├─ 校验浏览器提交的质检结果
  ├─ 调用 DeepSeek 生成解释和分析报告
  └─ MyBatis-Plus 访问 MySQL，Redis 保存会话和验证码
```

排产计算和大模型解释职责分离：OR-Tools 负责计算可执行方案，DeepSeek 负责解释计算结果，不直接决定生产计划。

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 前端 | Vue 3、TypeScript、Vite、Ant Design Vue、Pinia |
| 浏览器 AI | ONNX Runtime Web、WebGPU、WASM、Web Worker |
| 后端 | Java 17、Spring Boot 3.3.5、Spring Security、WebSocket |
| 数据访问 | MyBatis-Plus 3.5.7、MySQL 8.x |
| 缓存与会话 | Redis 6+/7.x |
| 智能排产 | OR-Tools CP-SAT |
| 结果解释 | DeepSeek API，默认模型为 `deepseek-v4-flash` |
| 部署 | Docker、Docker Compose、Nginx、Supervisor |

## 运行环境

| 依赖 | 版本要求 | 用途 |
| --- | --- | --- |
| 操作系统 | Windows 10/11、Linux 或 macOS | 开发和运行环境 |
| JDK | 17 | 启动后端 |
| Maven | 3.8+，推荐 3.9+ | 后端构建和启动 |
| Node.js | 20.19+ 或 22.12+ | 前端构建和启动 |
| npm | 随 Node.js 安装 | 前端依赖管理 |
| MySQL | 8.x | 业务数据存储 |
| Redis | 6+，推荐 7.x | 登录会话、验证码和在线状态 |
| Chrome/Edge | 较新版本 | 前端和浏览器端质检 |

### GPU、CUDA 和显卡要求

本项目不要求 NVIDIA 显卡、CUDA、cuDNN 或 NVIDIA Container Toolkit。

- 质检模型在浏览器端运行；
- 浏览器支持 WebGPU 时，优先使用运行浏览器的本机 GPU；
- 不支持 WebGPU 时，自动回退到 WASM/CPU；
- 后端不执行 ONNX 推理；
- OR-Tools 排产算法使用 CPU；
- DeepSeek 通过网络 API 调用，不需要本地 GPU。

因此，远程服务器不需要配置 GPU。GPU 只可能影响执行质检的浏览器电脑上的推理速度。实时质检需要浏览器摄像头权限，远程访问建议使用 HTTPS。

## 目录结构

```text
backend/                         Spring Boot 后端
frontend/                        Vue 3 前端
docs/                            项目文档和 ONNX 模型
docs/database/                   数据库初始化和迁移脚本
docs/best.onnx                  浏览器端质检模型
photo/upload/                    上传原图目录
photo/results/                   质检结果图目录
deploy/single-container/        单容器配置
Dockerfile                       单应用容器构建文件
docker-compose.single.yml       单应用容器 Compose 配置
docker-compose.prod.yml          前端、后端、Redis 多容器配置
Jenkinsfile                      Jenkins 流水线
```

## 本地启动

### 1. 获取代码

```bash
git clone https://github.com/supermlk500-del/saas.git
cd saas
```

### 2. 初始化 MySQL

创建数据库：

```sql
CREATE DATABASE zhihuitong
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

导入数据库结构和基础权限数据：

```bash
mysql -uroot -p zhihuitong < docs/database/zhihuitong.sql
```

全新安装通常只需要导入 `zhihuitong.sql`。升级旧版本时，再按版本执行 `docs/database/` 下的迁移脚本。

### 3. 启动 Redis

使用 Docker 启动无密码 Redis：

```bash
docker run -d --name zhihuitong-redis --restart unless-stopped -p 6379:6379 redis:7-alpine
```

如果 Redis 设置了密码，需要在 `.env` 中同步设置 `REDIS_PASSWORD`。

### 4. 配置环境变量

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

配置说明：

- `JWT_SECRET` 至少 32 个字符，否则后端无法启动；
- `DEEPSEEK_API_KEY` 用于生成排产解释和质量分析报告；
- 未配置 DeepSeek Key 时，核心业务和部分规则分析仍可运行；
- `CAPTCHA_ENABLED` 默认开启，本地调试时可以临时设置为 `false`；
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

前端默认地址：`http://localhost:5173`。开发环境下，Vite 会把 `/prod-api` 请求代理到后端 `8080` 端口。

## Docker 部署

### 单应用容器

单应用容器包含 Nginx、Spring Boot 和浏览器端 ONNX 模型。MySQL 和 Redis 仍然作为外部服务运行。

准备环境变量文件：

```bash
cp deploy/single-container/env.example deploy/single-container/env
```

Windows PowerShell 使用：

```powershell
Copy-Item deploy/single-container/env.example deploy/single-container/env
```

编辑 `deploy/single-container/env`，填写数据库、Redis、JWT 和 DeepSeek 配置，然后执行：

```bash
docker compose -f docker-compose.single.yml up -d --build
```

应用访问地址：`http://localhost:18080`。

查看日志：

```bash
docker logs --tail=200 zhihuitong
```

停止容器：

```bash
docker compose -f docker-compose.single.yml down
```

单容器会将本地 `photo/` 挂载到容器的 `/app/data`，上传图片和质检结果会持久化在宿主机。

### 多容器生产部署

`docker-compose.prod.yml` 启动以下服务：

```text
saas-frontend + saas-backend + saas-redis + 外部 MySQL
```

准备服务器目录：

```text
/opt/saas/docs/best.onnx
/opt/saas/data/upload
/opt/saas/data/results
/opt/saas/redis
```

注意：`/opt/saas/docs` 会覆盖容器内的 `/app/docs`，因此必须将 `best.onnx` 放在 `/opt/saas/docs` 中。

配置根目录 `.env` 后启动：

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

## AI 运行说明

### 浏览器端 ONNX 质检

运行模型为 `docs/best.onnx`。后端会校验模型文件大小和 SHA-256，并向浏览器提供 Manifest。不要随意替换模型，否则可能出现模型校验失败。

推理链路如下：

```text
图片或视频帧
  → Web Worker 预处理
  → ONNX Runtime Web 推理
  → 后处理和检测框绘制
  → 人工复核
  → 后端校验并保存质检记录
```

### 智能排产和质量分析

OR-Tools 根据交期、工序顺序、设备能力、设备占用和生产约束生成排产方案。DeepSeek 负责解释排产结果、质量风险和可能根因，最终业务方案仍由人工确认。

## 端口

| 端口 | 服务 | 使用场景 |
| --- | --- | --- |
| 5173 | Vite 前端 | 本地开发 |
| 8080 | Spring Boot 后端 | 本地开发 |
| 18080 | Nginx 应用入口 | 单容器部署 |
| 3306 | MySQL | 数据库服务 |
| 6379 | Redis | 会话和缓存服务 |

## 常见问题

### 后端提示 `JWT secret must be at least 32 bytes`

设置长度不少于 32 个字符的 `JWT_SECRET`，然后重启后端。

### 验证码不显示或登录状态失效

检查后端是否启动、Redis 是否运行，以及 `REDIS_HOST`、`REDIS_PORT` 和 `REDIS_PASSWORD` 是否正确。

### 质检模型加载失败

检查 `docs/best.onnx`、`AI_YOLO_MODEL_PATH`，以及多容器部署时的 `/opt/saas/docs/best.onnx`。

### DeepSeek 没有生成报告

检查 `DEEPSEEK_ENABLED`、`DEEPSEEK_API_KEY`、`DEEPSEEK_MODEL`，并确认运行环境可以访问 `https://api.deepseek.com`。

### 实时质检无法打开摄像头

检查浏览器摄像头权限。远程访问时使用 HTTPS，本机使用 `localhost` 测试。

## 安全要求

以下内容不能提交到 Git：

- MySQL 密码；
- Redis 密码；
- JWT 密钥；
- DeepSeek API Key；
- 服务器私钥和证书。

如果 API Key 曾经提交到公开仓库，应立即撤销并重新生成。

## 项目文档

- [开发文档目录](docs/README.md)
- [快速启动](docs/02-快速启动.md)
- [系统架构](docs/03-系统架构.md)
- [核心业务流程](docs/04-核心业务流程.md)
- [接口说明](docs/07-接口说明.md)
- [数据库说明](docs/08-数据库说明.md)
- [AI 能力说明](docs/10-AI能力说明.md)
- [部署运维](docs/12-部署运维.md)
- [Jenkins 单容器部署](docs/16-Jenkins单容器部署.md)
