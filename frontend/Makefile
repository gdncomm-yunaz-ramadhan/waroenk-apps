# ===========================================
# Waroenk Frontend - Makefile
# ===========================================

.PHONY: all help install dev build preview clean \
        docker-build docker-dev docker-prod docker-stop docker-logs docker-clean ensure-network

# Default target
.DEFAULT_GOAL := all

# Colors
GREEN  := \033[0;32m
YELLOW := \033[0;33m
BLUE   := \033[0;34m
NC     := \033[0m

# Variables
IMAGE_NAME := waroenk-frontend
DEV_PORT := 5173
PROD_PORT := 3000

# ===========================================
# Default: Build and run production
# ===========================================
all: docker-stop docker-build-prod docker-prod
	@echo ""
	@echo "$(GREEN)=========================================$(NC)"
	@echo "$(GREEN)Frontend running in PRODUCTION mode$(NC)"
	@echo "$(GREEN)URL: http://localhost:$(DEV_PORT)$(NC)"
	@echo "$(GREEN)Resources: CPU 0.5 core, Memory 256MB$(NC)"
	@echo "$(GREEN)=========================================$(NC)"

# ===========================================
# Help
# ===========================================
help:
	@echo ""
	@echo "$(BLUE)Waroenk Frontend$(NC)"
	@echo ""
	@echo "$(GREEN)Quick Start:$(NC)"
	@echo "  make all         - Build & run production (default)"
	@echo ""
	@echo "$(GREEN)Local Development:$(NC)"
	@echo "  make install     - Install dependencies"
	@echo "  make dev         - Start development server (port $(DEV_PORT))"
	@echo "  make build       - Build for production"
	@echo "  make preview     - Preview production build"
	@echo "  make clean       - Clean build artifacts"
	@echo ""
	@echo "$(GREEN)Docker Commands:$(NC)"
	@echo "  make docker-build      - Build all Docker images"
	@echo "  make docker-build-prod - Build production image only"
	@echo "  make docker-dev        - Run in development mode (port $(DEV_PORT))"
	@echo "  make docker-prod       - Run in production mode (port $(PROD_PORT))"
	@echo "  make docker-stop       - Stop all containers"
	@echo "  make docker-logs       - View container logs"
	@echo "  make docker-clean      - Remove containers and images"
	@echo ""

# ===========================================
# Local Development
# ===========================================
install:
	@echo "$(BLUE)Installing dependencies...$(NC)"
	npm ci

dev:
	@echo "$(BLUE)Starting development server on port $(DEV_PORT)...$(NC)"
	npm run dev -- --port $(DEV_PORT) --strictPort

build:
	@echo "$(BLUE)Building for production...$(NC)"
	npm run build -- --port $(DEV_PORT) --strictPort

preview:
	@echo "$(BLUE)Previewing production build...$(NC)"
	npm run preview

clean:
	@echo "$(YELLOW)Cleaning build artifacts...$(NC)"
	rm -rf dist node_modules/.vite

# ===========================================
# Docker Commands
# ===========================================
docker-build:
	@echo "$(BLUE)Building Docker images...$(NC)"
	docker build --no-cache -t $(IMAGE_NAME):latest --target production .
	docker build --no-cache -t $(IMAGE_NAME):dev --target development .

docker-build-prod:
	@echo "$(BLUE)Building production Docker image...$(NC)"
	docker build --no-cache -t $(IMAGE_NAME):latest --target production .

docker-build-dev:
	@echo "$(BLUE)Building development Docker image...$(NC)"
	docker build --no-cache -t $(IMAGE_NAME):dev --target development .

ensure-network:
	@if [ -z "$$(docker network ls --filter name=^backend$$ -q)" ]; then \
		echo "➡️  Creating network backend"; \
		docker network create backend; \
	fi

docker-dev: ensure-network
	@echo "$(BLUE)Starting development container on port $(DEV_PORT)...$(NC)"
	docker-compose --profile dev up -d frontend-dev
	@echo "$(GREEN)Frontend running at http://localhost:$(DEV_PORT)$(NC)"

docker-prod: ensure-network
	@echo "$(BLUE)Starting production container on port $(DEV_PORT)...$(NC)"
	docker-compose --profile prod up -d frontend
	@echo "$(GREEN)Frontend running at http://localhost:$(DEV_PORT)$(NC)"

docker-stop:
	@echo "$(YELLOW)Stopping containers...$(NC)"
	-docker-compose --profile dev --profile prod down 2>/dev/null || true

docker-logs:
	docker-compose logs -f

docker-logs-dev:
	docker logs -f waroenk-frontend-dev

docker-logs-prod:
	docker logs -f waroenk-frontend

docker-clean:
	@echo "$(YELLOW)Cleaning Docker resources...$(NC)"
	docker-compose --profile dev --profile prod down -v 2>/dev/null || true
	docker rmi $(IMAGE_NAME):latest $(IMAGE_NAME):dev 2>/dev/null || true

# ===========================================
# Combined Commands
# ===========================================
up: all

up-dev: docker-stop docker-build-dev docker-dev
	@echo ""
	@echo "$(GREEN)=========================================$(NC)"
	@echo "$(GREEN)Frontend running in DEVELOPMENT mode$(NC)"
	@echo "$(GREEN)URL: http://localhost:$(DEV_PORT)$(NC)"
	@echo "$(GREEN)Resources: CPU 1.0 core, Memory 1GB$(NC)"
	@echo "$(GREEN)=========================================$(NC)"

up-prod: all

restart: docker-stop all

restart-dev: docker-stop up-dev

down: docker-stop

status:
	@echo "$(BLUE)Container Status:$(NC)"
	@docker ps --filter "name=waroenk-frontend" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || echo "No containers running"

stats:
	@echo "$(BLUE)Resource Usage:$(NC)"
	@docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}" waroenk-frontend waroenk-frontend-dev 2>/dev/null || echo "No containers running"
