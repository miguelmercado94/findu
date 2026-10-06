# FINDU Authorization Server OAuth2 — imagen runtime (JRE 21).
#
# Build manual:
#   docker build -t findu-autorization-server-oauth2:local .

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring -g 1000 && adduser -S spring -u 1000 -G spring \
    && apk add --no-cache curl \
    && rm -rf /var/cache/apk/*

COPY build/libs/*.jar /app/app.jar

USER spring:spring

EXPOSE 9595

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS:-} -jar /app/app.jar"]

