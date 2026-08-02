FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app
ENV TZ=Asia/Shanghai
ENV SPRING_PROFILES_ACTIVE=prod

COPY --from=builder /workspace/target/zhihuitong-backend-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
