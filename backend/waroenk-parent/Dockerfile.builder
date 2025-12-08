# syntax=docker/dockerfile:1
# ================================
# Shared Builder - Maven + JDK 21
# OPTIMIZED: Single mvn install builds ALL modules
# ================================
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Configure Maven for maximum parallelism and speed
ENV MAVEN_OPTS="-Dmaven.artifact.threads=10 -Xmx2g -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -XX:+UseParallelGC"

# ================================
# LAYER 1: Copy only pom.xml files (cached unless pom.xml changes)
# ================================
COPY pom.xml .
COPY grpc-contract/pom.xml grpc-contract/
COPY member/pom.xml member/
COPY catalog/pom.xml catalog/
COPY cart/pom.xml cart/
COPY api-gateway/pom.xml api-gateway/

# ================================
# LAYER 2: Download ALL dependencies (cached unless pom.xml changes)
# Single command: install parent + resolve all deps
# ================================
RUN --mount=type=cache,target=/root/.m2/repository,sharing=locked \
    echo "============================================================" && \
    echo "[STEP 1/2] Installing parent POM + downloading dependencies" && \
    echo "============================================================" && \
    mvn -B -N install --no-transfer-progress && \
    mvn -B dependency:go-offline -T 4C --no-transfer-progress || true

# ================================
# LAYER 3: Copy ALL source files
# This layer and below rebuild on ANY code change
# ================================
COPY grpc-contract/src grpc-contract/src
COPY member/src member/src
COPY catalog/src catalog/src
COPY cart/src cart/src
COPY api-gateway/src api-gateway/src

# ================================
# LAYER 4: BUILD EVERYTHING IN ONE mvn install
# Maven automatically builds in correct order: grpc-contract -> member,catalog,cart,api-gateway
# Using -T 1C means 1 thread per CPU core (parallel module builds)
# ================================
RUN --mount=type=cache,target=/root/.m2/repository,sharing=locked \
    echo "============================================================" && \
    echo "[STEP 2/2] Building ALL modules in single Maven process..." && \
    echo "============================================================" && \
    mvn -B -DskipTests install \
        -T 1C \
        -Denforcer.skip=true \
        -Dmaven.javadoc.skip=true \
        -Dmaven.source.skip=true \
        --no-transfer-progress && \
    echo "" && \
    echo "============================================================" && \
    echo "[SUCCESS] All modules built!" && \
    echo "============================================================"

# ================================
# LAYER 5: Collect JARs to known location (fast, just file copies)
# ================================
RUN mkdir -p /app/jars && \
    cp member/target/member.jar /app/jars/ && \
    cp catalog/target/catalog.jar /app/jars/ && \
    cp cart/target/cart.jar /app/jars/ && \
    cp api-gateway/target/api-gateway.jar /app/jars/ && \
    echo "JARs collected:" && ls -lh /app/jars/
