# FINDU Notification Dispatcher — Spring Cloud Function como HTTP server (dev local).
# En producción se despliega como AWS Lambda real.
#
# Build: docker build -t findu-notification-dispatcher:local .

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring -g 1000 && adduser -S spring -u 1000 -G spring \
    && apk add --no-cache curl \
    && rm -rf /var/cache/apk/*

COPY build/libs/*.jar /app/app.jar

USER spring:spring

EXPOSE 9000

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS:-} -jar /app/app.jar"]

