.PHONY: all build build-fast compile clean up down logs help ensure-network mongodb redis infra infra-down

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
	@echo "Cart Module - Build & Run Commands"
	@echo ""
	@echo "Build Commands:"
	@echo "  make build         Build cart image (requires waroenk-builder)"
	@echo "  make build-fast    Compile locally + package (FAST) ⚡"
	@echo "  make compile       Just compile locally (no Docker)"
	@echo "  make clean         Remove built image and volumes"
	@echo ""
	@echo "Service Commands:"
	@echo "  make up            Start cart service (uses pre-built image)"
	@echo "  make down          Stop cart service"
	@echo "  make logs          View cart service logs"
	@echo "  make all           Start infra + build + run"
	@echo ""
	@echo "Infrastructure Commands:"
	@echo "  make infra         Start MongoDB + Redis"
	@echo "  make infra-down    Stop MongoDB + Redis"
	@echo "  make mongodb       Start MongoDB only"
	@echo "  make redis         Start Redis only"
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

infra: ensure-network mongodb redis
	@echo "✅ All infrastructure services started"

infra-down:
	@echo "🛑 Stopping infrastructure (keeps data)..."
	$(MAKE) -C ../../../infra/redis down
	$(MAKE) -C ../../../infra/mongodb down

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
	@echo "🔨 Building cart service image..."
	DOCKER_BUILDKIT=1 docker build --no-cache -f Dockerfile -t cart:latest ..
	@echo "✅ Done! Image: cart:latest"

# Fast build - compiles locally first, then packages into Docker (MUCH FASTER)
build-fast: ensure-network
	@echo "🚀 Fast build - compiling locally first..."
	cd .. && ./mvnw -DskipTests package -pl grpc-contract,cart -am --no-transfer-progress
	@echo "📦 Packaging into Docker image..."
	docker build -f Dockerfile.dev -t cart:latest .
	@echo "✅ Done! Image: cart:latest"

# Just compile locally (no Docker)
compile:
	@echo "🔨 Compiling cart locally..."
	cd .. && ./mvnw -DskipTests package -pl grpc-contract,cart -am

# ================================
# Service Commands
# ================================
up: ensure-network infra
	@echo "🚀 Starting cart service..."
	docker-compose -f $(COMPOSE_FILE) up -d
	@echo ""
	@echo "✅ Cart service started!"
	@echo "   HTTP: http://localhost:8082"
	@echo "   gRPC: localhost:9092"
	@echo "   Swagger: http://localhost:8082/swagger-ui.html"

down:
	@echo "🛑 Stopping cart service..."
	docker-compose -f $(COMPOSE_FILE) down -v

logs:
	docker-compose -f $(COMPOSE_FILE) logs -f cart

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
	@docker ps --filter "name=mongodb" --filter "name=redis" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# ================================
# All Target
# ================================
all: infra build up
