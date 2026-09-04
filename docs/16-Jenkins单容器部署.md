# Jenkins 单容器部署

## 方案说明

根目录 `Dockerfile` 会构建一个应用镜像，镜像内包含：

- Nginx：提供前端页面和反向代理；
- Spring Boot：提供后端接口；
- ONNX 模型：用于浏览器端智能质检；
- Supervisor：同时管理 Nginx 和 Spring Boot 两个进程。

MySQL 和 Redis 不放进应用容器，而是作为外部服务连接。这样应用容器可以随时替换，数据库和 Redis 数据不会随着容器删除而丢失。

## Jenkins 构建条件

Jenkins 节点需要安装并允许使用：

- Git；
- JDK 17 或 Maven；
- Node.js 22 和 npm；
- Docker，并且 Jenkins 用户有权限访问 Docker daemon。

仓库根目录的 `Jenkinsfile` 默认执行：

1. 后端 Maven 测试；
2. 前端 Vitest 测试；
3. 构建单容器镜像。

Jenkins Agent 如果是 Windows，需要把 `Jenkinsfile` 中的 `sh` 改成 `bat` 或 `powershell`；当前文件按 Linux Jenkins Agent 编写。

## 手动构建

在项目根目录执行：

```bash
docker build -t zhihuitong:latest .
```

## 启动容器

先复制环境变量模板：

```bash
cp deploy/single-container/env.example deploy/single-container/env
```

编辑 `deploy/single-container/env`，至少填写：

- `MYSQL_PASSWORD`；
- 长度不少于 32 个字符的 `JWT_SECRET`；
- `MYSQL_HOST` 和 `REDIS_HOST`。

然后执行：

```bash
docker run -d \
  --name zhihuitong \
  --restart unless-stopped \
  --add-host host.docker.internal:host-gateway \
  --env-file deploy/single-container/env \
  -p 18080:80 \
  -v /opt/saas/data:/app/data \
  zhihuitong:latest
```

访问：

```text
http://服务器地址:18080
```

如果 MySQL 或 Redis 运行在另一台机器，不能继续使用 `host.docker.internal`，应将环境变量改成对应服务器的内网地址，并确保防火墙允许访问 3306 和 6379。

## 使用 Docker Compose 启动

项目提供了只包含一个应用服务的 `docker-compose.single.yml`：

```bash
cp deploy/single-container/env.example deploy/single-container/env
docker compose -f docker-compose.single.yml up -d --build
```

## 检查状态

```bash
docker ps
docker logs --tail=200 zhihuitong
curl http://127.0.0.1:18080/prod-api/actuator/health
```

健康检查正常时应返回 Spring Boot 的 `UP` 状态。登录验证码关闭时，前端登录页不会要求填写验证码；如果开启验证码，应确认 Redis 连接正常。

## 数据持久化

应用容器只持久化业务上传和质检结果：

```text
/opt/saas/data/upload  -> /app/data/upload
/opt/saas/data/results -> /app/data/results
```

MySQL 数据由 MySQL 自己持久化，Redis 数据由 Redis 自己持久化。不要把数据库目录直接挂载到应用容器中。
