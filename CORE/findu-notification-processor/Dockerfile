FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /workspace
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle gradle
COPY src src
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew bootJar -x test -x check --no-daemon --warning-mode none

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring -g 1000 && adduser -S spring -u 1000 -G spring && apk add --no-cache curl && rm -rf /var/cache/apk/*
COPY --from=builder /workspace/build/libs/*.jar /app/app.jar
USER spring:spring
EXPOSE 8083
ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS:-} -jar /app/app.jar"]
