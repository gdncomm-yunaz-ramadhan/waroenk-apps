.PHONY: all build clean up down down-all stop reset reset-all logs help ensure-network postgresql redis infra infra-down infra-reset

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
	@echo "Member Module - Build & Run Commands"
	@echo ""
	@echo "Build Commands:"
	@echo "  make build         Build member image (requires waroenk-builder)"
	@echo "  make clean         Remove built image and volumes"
	@echo ""
	@echo "Service Commands:"
	@echo "  make up            Start member service (uses pre-built image)"
	@echo "  make down          Stop member service (keeps data)"
	@echo "  make down-all      Stop member + infrastructure (keeps data)"
	@echo "  make logs          View member service logs"
	@echo "  make all           Start infra + build + run"
	@echo ""
	@echo "Reset Commands (DESTRUCTIVE - deletes data):"
	@echo "  make reset         Stop member + delete its volumes"
	@echo "  make reset-all     Stop all + delete ALL data (DB, Redis, etc)"
	@echo ""
	@echo "Infrastructure Commands:"
	@echo "  make infra         Start PostgreSQL + Redis"
	@echo "  make infra-down    Stop infrastructure (keeps data)"
	@echo "  make infra-reset   Stop + DELETE all infrastructure data"
	@echo "  make postgresql    Start PostgreSQL only"
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
redis:
	$(MAKE) -C ../../../infra/redis up

postgresql:
	$(MAKE) -C ../../../infra/postgresql up

infra: ensure-network postgresql redis

infra-down:
	@echo "🛑 Stopping infrastructure (keeps data)..."
	$(MAKE) -C ../../../infra/redis down
	$(MAKE) -C ../../../infra/postgresql down

infra-reset:
	@echo "⚠️  WARNING: This will DELETE all infrastructure data!"
	$(MAKE) -C ../../../infra/redis reset
	$(MAKE) -C ../../../infra/postgresql reset

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
	@echo "🔨 Building member service image..."
	DOCKER_BUILDKIT=1 docker build --no-cache -f Dockerfile -t member:latest ..
	@echo "✅ Done! Image: member:latest"

# ================================
# Service Commands
# ================================
up: ensure-network infra
	@echo "🚀 Starting member service..."
	docker-compose -f $(COMPOSE_FILE) up -d

down:
	@echo "🛑 Stopping member service..."
	docker-compose -f $(COMPOSE_FILE) down

down-all: down infra-down

stop:
	@echo "🛑 Stopping member service (keeps volumes)..."
	docker-compose -f $(COMPOSE_FILE) down

reset:
	@echo "⚠️  WARNING: This will DELETE member service volumes!"
	docker-compose -f $(COMPOSE_FILE) down -v

reset-all: reset infra-reset

logs:
	docker-compose -f $(COMPOSE_FILE) logs -f member

# ================================
# Utility Commands
# ================================
clean:
	@echo "🧹 Cleaning up..."
	docker-compose -f $(COMPOSE_FILE) down -v --rmi all

# ================================
# All Target
# ================================
all: infra build up
