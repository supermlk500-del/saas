# Zhihuitong Backend

Java backend for Zhihuitong, based on Spring Boot, MyBatis-Plus, MySQL, WebSocket, and ONNX Runtime.

## Conventions

- Port: `8080`
- Frontend proxy prefix: `/prod-api`
- Database: `zhihuitong`
- Base package: `com.zhihuitong`

## Run locally

1. Create database `zhihuitong`
2. Import `../docs/zhihuitong.sql`
3. Set environment variables if needed:
   - `MYSQL_HOST`
   - `MYSQL_PORT`
   - `MYSQL_DATABASE`
   - `MYSQL_USERNAME`
   - `MYSQL_PASSWORD`
4. Start the service:

```bash
mvn spring-boot:run
```

## Implemented modules

- Order and order-item management
- Batch resource pool
- Process routes, process steps, machines, and capabilities
- Production planning and scheduling
- Offline image and realtime WebSocket quality inspection
- Exception and rework workflow
- Unified response, validation, pagination, CORS, and health checks

## Current boundaries

- No authentication or authorization module is implemented yet
- Database schema changes are not managed by Flyway or Liquibase
- Production deployment and CI/CD configuration are not included

## Suggested next steps

- Add authentication and role-based authorization
- Add Flyway or Liquibase for schema migrations
- Add integration tests for the main order-to-quality workflow
- Add production deployment and CI/CD configuration
