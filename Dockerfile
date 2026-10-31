# syntax=docker/dockerfile:1

# ---------- Stage 1: build ----------
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src src
# Maven cache rebuilds ko tez rakhta hai. Tests CI mein chalte hain (unhe DB chahiye), image build mein nahi.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q -DskipTests package && cp target/*.jar /workspace/app.jar

# ---------- Stage 2: run ----------
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S -G app app \
    && mkdir -p /app/uploads/resumes \
    && chown -R app:app /app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/app.jar app.jar
USER app

ENV SERVER_PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod \
    STORAGE_DIR=/app/uploads/resumes \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]