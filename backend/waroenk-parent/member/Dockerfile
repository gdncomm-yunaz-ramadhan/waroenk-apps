# syntax=docker/dockerfile:1
# ================================
# Member Service - Using Eclipse Temurin JRE Alpine
# ~200MB total image size
# ================================

FROM waroenk-builder:latest AS builder

# Runtime Stage - Eclipse Temurin JRE on Alpine
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Add minimal dependencies
RUN apk add --no-cache curl

# Copy application JAR from builder
COPY --from=builder /app/jars/member.jar app.jar

EXPOSE 8080 9090

# Optimized JVM settings for containers
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-XX:+UseStringDeduplication", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
