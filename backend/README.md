# Zhihuitong Backend

Initial backend scaffold for `Zhihuitong` based on `Spring Boot + MyBatis-Plus + MySQL`.

## Conventions

- Port: `8080`
- Frontend proxy prefix: `/prod-api`
- Database: `zhihuitong`
- Base package: `com.zhihuitong`

## Run locally

1. Create database `zhihuitong`
2. Import `zhihuitong_db.sql` from the repository root
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

## Included in this scaffold

- Spring Boot app bootstrap
- MyBatis-Plus pagination config
- Global exception handler
- Unified response model
- CORS config
- Health check endpoint
- Placeholder auth endpoints compatible with the current frontend flow

## Suggested next steps

- Replace placeholder auth with JWT-based auth
- Split business modules by controller/service/mapper/entity
- Add Flyway or Liquibase for schema migrations
- Implement real database-backed APIs
