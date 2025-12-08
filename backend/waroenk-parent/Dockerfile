# syntax=docker/dockerfile:1
# ================================
# Parent Multi-Module Build Dockerfile
# Builds all modules in dependency order
# ================================

# ================================
# 1) Build Stage - Build all modules
# ================================
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Configure Maven for faster builds
ENV MAVEN_OPTS="-Dmaven.artifact.threads=10 -Xmx1024m -XX:+TieredCompilation -XX:TieredStopAtLevel=1"

# Copy Maven wrapper and config
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw

# Copy all module pom.xml files for dependency resolution
COPY grpc-contract/pom.xml grpc-contract/
COPY member/pom.xml member/
COPY catalog/pom.xml catalog/
COPY cart/pom.xml cart/
COPY api-gateway/pom.xml api-gateway/

# Install parent POM first (required for child modules to resolve parent)
RUN --mount=type=cache,target=/root/.m2/repository \
    ./mvnw -B -N install --no-transfer-progress

# Cache dependencies
RUN --mount=type=cache,target=/root/.m2/repository \
    ./mvnw dependency:go-offline -B --no-transfer-progress || true

# Copy all source files
COPY grpc-contract/src grpc-contract/src
COPY member/src member/src
COPY catalog/src catalog/src
COPY cart/src cart/src
COPY api-gateway/src api-gateway/src

# Build all modules in order (grpc-contract first, then others)
# Install grpc-contract to local repo first so siblings can use it
RUN --mount=type=cache,target=/root/.m2/repository \
    ./mvnw -B -DskipTests install -pl grpc-contract --no-transfer-progress && \
    ./mvnw -B -DskipTests package -pl member,catalog,cart,api-gateway --no-transfer-progress

# ================================
# 2) Custom JRE Image Stage
# ================================
FROM eclipse-temurin:21-jdk-jammy AS jlink-build
WORKDIR /jlink-jre
RUN jlink --add-modules ALL-MODULE-PATH --output custom-jre --compress=2 --no-header-files --no-man-pages

# ================================
# 3) Artifact Staging
# ================================
FROM scratch AS artifacts
COPY --from=build /app/grpc-contract/target/grpc-contract.jar /grpc-contract.jar
COPY --from=build /app/member/target/member.jar /member.jar
COPY --from=build /app/catalog/target/catalog.jar /catalog.jar
COPY --from=build /app/cart/target/cart.jar /cart.jar
COPY --from=build /app/api-gateway/target/api-gateway.jar /api-gateway.jar
COPY --from=jlink-build /jlink-jre/custom-jre /custom-jre

# ===========================
# 4) Default Runtime (Member)
# ===========================
FROM gcr.io/distroless/java21-debian12 AS runtime
COPY --from=jlink-build /jlink-jre/custom-jre /opt/java/openjdk
WORKDIR /app
ENV PATH="/opt/java/openjdk/bin:${PATH}"

# ===========================
# Module-specific runtime targets
# ===========================
FROM runtime AS member-runtime
COPY --from=build /app/member/target/member.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM runtime AS catalog-runtime
COPY --from=build /app/catalog/target/catalog.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM runtime AS cart-runtime
COPY --from=build /app/cart/target/cart.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM runtime AS api-gateway-runtime
COPY --from=build /app/api-gateway/target/api-gateway.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
