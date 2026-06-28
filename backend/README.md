# Zhihuitong Backend

AI胚布排产质检系统后端，基于 Spring Boot、Spring Security、MyBatis-Plus、MySQL、Redis、WebSocket 和 ONNX Runtime。

## 本地启动

1. 按 [`../docs/开发指南.md`](../docs/开发指南.md) 初始化 MySQL、Redis 和环境变量。
2. 执行：

```powershell
mvn spring-boot:run
```

默认端口：`8080`。健康检查：`GET /actuator/health`。

## 测试

```powershell
mvn test
```

完整架构、API、数据库和权限说明统一维护在 [`../docs/README.md`](../docs/README.md)。