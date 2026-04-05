# ════════════════════════════════════════════════════════════════════════════
#  STAGE 1: Build  (Maven + JDK 17)
# ════════════════════════════════════════════════════════════════════════════
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper + pom first (layer cache — only re-downloads when pom changes)
COPY mvnw pom.xml ./
COPY .mvn .mvn

# Fix execute permission (lost when committed from Windows)
RUN chmod +x mvnw

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ════════════════════════════════════════════════════════════════════════════
#  STAGE 2: Run  (JRE only — smaller image)
# ════════════════════════════════════════════════════════════════════════════
FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

# Always use prod profile in container (Render / Docker deployments)
ENV SPRING_PROFILES_ACTIVE=prod

# Create non-root user (security best practice)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy the fat JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Health check — uses dynamic PORT (Render injects it; default 8080 locally)
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
  CMD wget -qO- http://localhost:${PORT:-8080}/actuator/health || exit 1

EXPOSE 8080

# ── JVM flags tuned for Render / Railway 512 MB free-tier containers ─────────
# SerialGC uses ~50 MB less overhead than G1GC — critical at this memory level.
# Fixed heap (-Xmx/-Xms) avoids the JVM over-committing and triggering OOM kill.
# TieredStopAtLevel=1 disables the expensive JIT C2 compiler during startup,
# halving the code-cache and metaspace pressure at boot time.
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:+UseSerialGC", \
  "-Xmx220m", \
  "-Xms64m", \
  "-Xss256k", \
  "-XX:MaxMetaspaceSize=120m", \
  "-XX:CompressedClassSpaceSize=24m", \
  "-XX:ReservedCodeCacheSize=32m", \
  "-XX:+TieredCompilation", \
  "-XX:TieredStopAtLevel=1", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.backgroundpreinitializer.ignore=true", \
  "-jar", "app.jar"]
