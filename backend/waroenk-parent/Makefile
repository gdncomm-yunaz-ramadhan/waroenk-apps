.PHONY: all build build-fresh build-verbose build-services clean up down logs help \
        ensure-network \
        up-member up-catalog up-cart up-gateway \
        down-member down-catalog down-cart down-gateway

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
	@echo "Waroenk Backend - Shared Builder Approach"
	@echo ""
	@echo "Build Commands (Two-Phase):"
	@echo "  make build            Build shared builder with cache (clean output)"
	@echo "  make build-fresh      Build without cache (fresh build)"
	@echo "  make build-verbose    Build with full Maven logs (for debugging)"
	@echo "  make build-services   Build all service images (uses pre-built JARs)"
	@echo "  make all              Build everything: builder + services"
	@echo ""
	@echo "Service Commands:"
	@echo "  make up               Start all services"
	@echo "  make down             Stop all services"
	@echo "  make logs             View logs from all services"
	@echo ""
	@echo "Individual Service Commands:"
	@echo "  make up-member        Start only member service"
	@echo "  make up-catalog       Start only catalog service"
	@echo "  make up-cart          Start only cart service"
	@echo "  make up-gateway       Start only api-gateway service"
	@echo ""
	@echo "Utility Commands:"
	@echo "  make clean            Remove all built images"
	@echo "  make ensure-network   Create shared network if not exists"
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
# Build Commands (Two-Phase)
# ================================

# Phase 1: Build the shared builder image (compiles ALL modules)
# Uses Docker layer caching for fast incremental builds
build:
	@echo ""
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo "  🔨 Phase 1: Building Shared Builder (ALL modules)"
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo ""
	@DOCKER_BUILDKIT=1 docker build \
		-f Dockerfile.builder \
		-t $(BUILDER_IMAGE):latest \
		.
	@echo ""
	@echo "✅ Builder image ready: $(BUILDER_IMAGE):latest"

# Force rebuild without cache (use when you need completely fresh build)
build-fresh:
	@echo ""
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo "  🔨 FRESH Build (no cache)"
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo ""
	@DOCKER_BUILDKIT=1 docker build \
		--no-cache \
		-f Dockerfile.builder \
		-t $(BUILDER_IMAGE):latest \
		.
	@echo ""
	@echo "✅ Builder image ready: $(BUILDER_IMAGE):latest"

# Build with verbose output (for debugging Maven/compilation issues)
build-verbose:
	@echo ""
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo "  🔨 Building (VERBOSE MODE - for debugging)"
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo ""
	DOCKER_BUILDKIT=1 docker build \
		--progress=plain \
		-f Dockerfile.builder \
		-t $(BUILDER_IMAGE):latest \
		.
	@echo ""
	@echo "✅ Builder image ready: $(BUILDER_IMAGE):latest"

# Phase 2: Build all service images (just copy pre-built JARs)
build-services: ensure-network
	@echo ""
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo "  📦 Phase 2: Building Service Images (lightweight)"
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo ""
	DOCKER_BUILDKIT=1 docker-compose -f $(COMPOSE_FILE) build --parallel
	@echo ""
	@echo "✅ All service images built!"

# Build everything
all: build build-services
	@echo ""
	@echo "✅ All builds complete!"
	@echo ""
	@echo "Run 'make up' to start services"

# ================================
# Service Up Commands
# ================================
up: ensure-network
	@echo "🚀 Starting all services..."
	docker-compose -f $(COMPOSE_FILE) up -d member catalog cart api-gateway
	@echo ""
	@echo "✅ Services started!"
	@echo "   Member:      http://localhost:8080"
	@echo "   Catalog:     http://localhost:8081"
	@echo "   Cart:        http://localhost:8082"
	@echo "   API Gateway: http://localhost:8000"

up-member: ensure-network
	@echo "🚀 Starting member service..."
	docker-compose -f $(COMPOSE_FILE) up -d member

up-catalog: ensure-network
	@echo "🚀 Starting catalog service..."
	docker-compose -f $(COMPOSE_FILE) up -d catalog

up-cart: ensure-network
	@echo "🚀 Starting cart service..."
	docker-compose -f $(COMPOSE_FILE) up -d cart

up-gateway: ensure-network
	@echo "🚀 Starting api-gateway service..."
	docker-compose -f $(COMPOSE_FILE) up -d api-gateway

# ================================
# Service Down Commands
# ================================
down:
	@echo "🛑 Stopping all services..."
	docker-compose -f $(COMPOSE_FILE) down

down-member:
	@echo "🛑 Stopping member service..."
	docker-compose -f $(COMPOSE_FILE) stop member
	docker-compose -f $(COMPOSE_FILE) rm -f member

down-catalog:
	@echo "🛑 Stopping catalog service..."
	docker-compose -f $(COMPOSE_FILE) stop catalog
	docker-compose -f $(COMPOSE_FILE) rm -f catalog

down-cart:
	@echo "🛑 Stopping cart service..."
	docker-compose -f $(COMPOSE_FILE) stop cart
	docker-compose -f $(COMPOSE_FILE) rm -f cart

down-gateway:
	@echo "🛑 Stopping api-gateway service..."
	docker-compose -f $(COMPOSE_FILE) stop api-gateway
	docker-compose -f $(COMPOSE_FILE) rm -f api-gateway

# ================================
# Utility Commands
# ================================
logs:
	docker-compose -f $(COMPOSE_FILE) logs -f

logs-member:
	docker-compose -f $(COMPOSE_FILE) logs -f member

logs-catalog:
	docker-compose -f $(COMPOSE_FILE) logs -f catalog

logs-cart:
	docker-compose -f $(COMPOSE_FILE) logs -f cart

logs-gateway:
	docker-compose -f $(COMPOSE_FILE) logs -f api-gateway

clean:
	@echo "🧹 Cleaning up..."
	docker-compose -f $(COMPOSE_FILE) down -v --rmi all
	docker rmi $(BUILDER_IMAGE):latest 2>/dev/null || true
	@echo "✅ Cleanup complete!"

status:
	@echo "📊 Service Status:"
	@docker-compose -f $(COMPOSE_FILE) ps
