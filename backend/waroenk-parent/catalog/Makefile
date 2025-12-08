.PHONY: all build build-fast compile clean up down logs help ensure-network mongodb redis typesense infra infra-down seed amazon-seed status

# ================================
# Configuration
# ================================
NETWORK_NAME=shared-network
COMPOSE_FILE=docker-compose.yml
BUILDER_IMAGE=waroenk-builder

# ================================
# Help
# ================================
help:
	@echo "Catalog Module - Build & Run Commands"
	@echo ""
	@echo "Build Commands:"
	@echo "  make build         Build catalog image (requires waroenk-builder)"
	@echo "  make build-fast    Compile locally + package (FAST) ⚡"
	@echo "  make compile       Just compile locally (no Docker)"
	@echo "  make clean         Remove built image and volumes"
	@echo ""
	@echo "Service Commands:"
	@echo "  make up            Start catalog service (uses pre-built image)"
	@echo "  make down          Stop catalog service"
	@echo "  make logs          View catalog service logs"
	@echo "  make all           Start infra + build + run"
	@echo "  make seed          Run with Faker to generate 100k products"
	@echo "  make amazon-seed   Run with real Amazon CSV data (~10k products)"
	@echo ""
	@echo "Infrastructure Commands:"
	@echo "  make infra         Start MongoDB + Redis + Typesense"
	@echo "  make infra-down    Stop MongoDB + Redis + Typesense"
	@echo "  make mongodb       Start MongoDB only"
	@echo "  make redis         Start Redis only"
	@echo "  make typesense     Start Typesense only"
	@echo ""
	@echo "Note: Run 'make build' from root to build all images first."
	@echo ""

# ================================
# Network Management
# ================================
ensure-network:
	@if [ -z "$$(docker network ls --filter name=^$(NETWORK_NAME)$$ -q)" ]; then \
		echo "➡️  Creating network $(NETWORK_NAME)"; \
		docker network create $(NETWORK_NAME); \
	else \
		echo "✔️  Network $(NETWORK_NAME) already exists"; \
	fi

# ================================
# Infrastructure
# ================================
mongodb:
	@echo "🍃 Starting MongoDB..."
	$(MAKE) -C ../../../infra/mongodb up

redis:
	@echo "🔴 Starting Redis..."
	$(MAKE) -C ../../../infra/redis up

typesense:
	@echo "🔍 Starting Typesense..."
	$(MAKE) -C ../../../infra/typesense up

infra: ensure-network mongodb redis typesense
	@echo "✅ All infrastructure services started"

infra-down:
	@echo "🛑 Stopping infrastructure (keeps data)..."
	$(MAKE) -C ../../../infra/redis down
	$(MAKE) -C ../../../infra/mongodb down
	$(MAKE) -C ../../../infra/typesense down

# ================================
# Build Commands
# ================================

# Build this service's Docker image (requires waroenk-builder to exist)
# For full rebuild, use root: make build
build: ensure-network
	@if [ -z "$$(docker images -q $(BUILDER_IMAGE):latest 2>/dev/null)" ]; then \
		echo "❌ Builder image not found. Run 'make build' from root first."; \
		exit 1; \
	fi
	@echo "🔨 Building catalog service image..."
	DOCKER_BUILDKIT=1 docker build --no-cache -f Dockerfile -t catalog:latest ..
	@echo "✅ Done! Image: catalog:latest"

# Fast build - compiles locally first, then packages into Docker (MUCH FASTER)
build-fast: ensure-network
	@echo "🚀 Fast build - compiling locally first..."
	cd .. && ./mvnw -DskipTests package -pl grpc-contract,catalog -am --no-transfer-progress
	@echo "📦 Packaging into Docker image..."
	docker build -f Dockerfile.dev -t catalog:latest .
	@echo "✅ Done! Image: catalog:latest"

# Just compile locally (no Docker)
compile:
	@echo "🔨 Compiling catalog locally..."
	cd .. && ./mvnw -DskipTests package -pl grpc-contract,catalog -am

# ================================
# Service Commands
# ================================
up: ensure-network infra
	@echo "🚀 Starting catalog service..."
	docker-compose -f $(COMPOSE_FILE) up -d
	@echo ""
	@echo "✅ Catalog service started!"
	@echo "   HTTP: http://localhost:8081"
	@echo "   gRPC: localhost:9091"
	@echo "   Swagger: http://localhost:8081/swagger-ui.html"

down:
	@echo "🛑 Stopping catalog service..."
	docker-compose -f $(COMPOSE_FILE) down -v

logs:
	docker-compose -f $(COMPOSE_FILE) logs -f catalog

# ================================
# Data Seeding
# ================================
seed: ensure-network infra
	@echo "🌱 Starting catalog service with seed profile..."
	@echo "   This will generate 100k products, 1k brands, 100 merchants..."
	SPRING_PROFILES_ACTIVE=seed docker-compose -f $(COMPOSE_FILE) up -d
	@echo ""
	@echo "📊 Watch seeding progress with: make logs"

amazon-seed: ensure-network infra
	@echo "🛒 Starting catalog service with Amazon CSV data..."
	@echo "   This will import ~10k real Amazon products..."
	SPRING_PROFILES_ACTIVE=amazon-seed docker-compose -f $(COMPOSE_FILE) up -d
	@echo ""
	@echo "📊 Watch seeding progress with: make logs"

# ================================
# Utility Commands
# ================================
clean:
	@echo "🧹 Cleaning up..."
	docker-compose -f $(COMPOSE_FILE) down -v --rmi all

status:
	@echo "📊 Service Status:"
	@docker-compose -f $(COMPOSE_FILE) ps
	@echo ""
	@echo "Infrastructure:"
	@docker ps --filter "name=mongodb" --filter "name=redis" --filter "name=typesense" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# ================================
# All Target
# ================================
all: infra build up
