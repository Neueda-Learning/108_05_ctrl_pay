# Multi-stage Dockerfile for Ctrl-Pay Payment Processing System
# Stage 1: Build with Maven
# Stage 2: Runtime with OpenJDK 17

# ============================================
# Stage 1: Build Stage
# ============================================
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder

LABEL stage=builder

# Set working directory
WORKDIR /build

# Copy pom.xml and download dependencies
COPY pom.xml .

# Download Maven dependencies (layer caching)
RUN mvn dependency:resolve

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# ============================================
# Stage 2: Runtime Stage
# ============================================
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="Neueda Development Team"
LABEL description="Ctrl-Pay Payment Processing System REST API"
LABEL version="0.0.1"

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder --chown=appuser:appgroup /build/target/ctrl_pay-*.jar /app/app.jar

# Use non-root user to run the application
USER appuser

# Expose application port
EXPOSE 8080

# Health check endpoint
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health/liveness || exit 1

# Environment variables (can be overridden at runtime)
ENV SPRING_PROFILES_ACTIVE=docker \
    JAVA_OPTS="-Xmx512m -Xms256m"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

