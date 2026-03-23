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

# Create non-root user (security best practice)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy the fat JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Actuator health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]

