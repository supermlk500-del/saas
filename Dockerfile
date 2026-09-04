# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-17 AS backend-builder

WORKDIR /workspace/backend
COPY backend/pom.xml ./

COPY backend/src ./src
RUN mvn -B -Dmaven.test.skip=true package \
    && cp target/*.jar /workspace/app.jar


FROM node:22-alpine AS frontend-builder

WORKDIR /workspace/frontend
COPY frontend/package*.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build


FROM eclipse-temurin:17-jre-jammy

ENV TZ=Asia/Shanghai \
    SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8081

RUN apt-get update \
    && apt-get install -y --no-install-recommends nginx supervisor ca-certificates curl \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /app/data/upload /app/data/results /app/docs /var/log/supervisor

WORKDIR /app

COPY --from=backend-builder /workspace/app.jar /app/app.jar
COPY --from=frontend-builder /workspace/frontend/dist /usr/share/nginx/html
COPY docs/best.onnx /app/docs/best.onnx
COPY deploy/single-container/nginx.conf /etc/nginx/sites-enabled/default
COPY deploy/single-container/supervisord.conf /etc/supervisor/conf.d/supervisord.conf
COPY deploy/single-container/entrypoint.sh /usr/local/bin/zhihuitong-entrypoint.sh

RUN chmod +x /usr/local/bin/zhihuitong-entrypoint.sh \
    && rm -f /etc/nginx/sites-enabled/default.bak

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
    CMD curl -fsS http://127.0.0.1/prod-api/actuator/health || exit 1

ENTRYPOINT ["/usr/local/bin/zhihuitong-entrypoint.sh"]
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]
