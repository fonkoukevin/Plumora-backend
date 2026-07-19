# --- Build stage -------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy the POM first and warm the dependency cache so this layer only invalidates when
# dependencies change, not on every source edit.
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src

RUN mvn -B -ntp clean package -DskipTests

# --- Runtime stage -------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Dedicated non-root user. The uploads directory is created and chowned here (not left for
# Docker to create on first volume mount) so a fresh named volume inherits the right owner
# instead of defaulting to root, which the app user could not then write into.
RUN addgroup -S plumora \
	&& adduser -S plumora -G plumora \
	&& mkdir -p /app/uploads \
	&& chown -R plumora:plumora /app

COPY --from=build --chown=plumora:plumora /app/target/*.jar app.jar

USER plumora:plumora

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
	CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/actuator/health || exit 1

# Exec form so the JVM runs as PID 1 and receives SIGTERM directly, which combined with
# server.shutdown=graceful (application-prod.yml) allows in-flight requests to drain.
ENTRYPOINT ["java", "-jar", "app.jar"]
