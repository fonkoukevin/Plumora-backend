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

# Dedicated non-root user. su-exec lets docker-entrypoint.sh (run as root - see USER below)
# drop to this user right before exec'ing the JVM, after it has fixed /app/uploads'
# ownership - see docker-entrypoint.sh for why that step is needed on every start, not just
# baked in here once at build time.
RUN apk add --no-cache su-exec \
	&& addgroup -S plumora \
	&& adduser -S plumora -G plumora \
	&& mkdir -p /app/uploads \
	&& chown -R plumora:plumora /app

COPY --from=build --chown=plumora:plumora /app/target/*.jar app.jar
COPY --chown=plumora:plumora docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

# Deliberately root here, not plumora: docker-entrypoint.sh needs root to chown the mounted
# /app/uploads volume before it drops privileges itself (su-exec) to run the JVM as plumora -
# the actual application process never runs as root, only this brief startup step does.
USER root

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
	CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/actuator/health || exit 1

# Exec form so the JVM (started by docker-entrypoint.sh via su-exec) runs as PID 1 and
# receives SIGTERM directly, which combined with server.shutdown=graceful
# (application-prod.yml) allows in-flight requests to drain.
ENTRYPOINT ["/app/docker-entrypoint.sh"]
