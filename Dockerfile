# syntax=docker/dockerfile:1
ARG JAR_SOURCE=source-build

FROM maven:3.9-eclipse-temurin-21 AS source-build
WORKDIR /workspace
COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY src src
RUN chmod +x mvnw && ./mvnw -B -DskipTests package \
    && mkdir /artifact && cp target/servicehub-api.jar /artifact/servicehub-api.jar

# CI selects this stage to package the JAR that already passed verify.
FROM scratch AS prebuilt
COPY target/servicehub-api.jar /artifact/servicehub-api.jar

FROM ${JAR_SOURCE} AS artifact

FROM eclipse-temurin:21-jre AS runtime
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=artifact --chown=10001:10001 /artifact/servicehub-api.jar app.jar
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD port="${PORT:-8080}"; port="${SERVER_PORT:-$port}"; \
    curl --fail --silent --show-error "http://127.0.0.1:$port/actuator/health" >/dev/null || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
