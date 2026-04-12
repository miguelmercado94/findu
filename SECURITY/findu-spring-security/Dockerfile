# FINDU Spring Security — imagen runtime (JRE 21).
# Perfil Spring (dev | qa | pdn) y resto de variables: .env + docker-compose (env_file / environment).
#
# Build manual:
#   docker build -t findu-spring-security:local .

FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /workspace

COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle gradle
COPY src src

RUN chmod +x gradlew \
    && ./gradlew bootJar -x test -x check --no-daemon --warning-mode none

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring -g 1000 && adduser -S spring -u 1000 -G spring \
    && apk add --no-cache curl \
    && rm -rf /var/cache/apk/*

COPY --from=builder /workspace/build/libs/*.jar /app/app.jar

USER spring:spring

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS:-} -jar /app/app.jar"]
