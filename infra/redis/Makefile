.PHONY: up down stop reset logs network

# Create shared network if not exists
network:
	@docker network inspect waroenk-network >/dev/null 2>&1 || docker network create waroenk-network
	@echo "✅ Network waroenk-network ready"

# Start Redis (Standalone)
up: network
	docker-compose up -d
	@echo "✅ Redis started (standalone mode)"
	@echo "   Redis: localhost:6379"

# Stop Redis (keeps data)
down:
	docker-compose down

# Alias for down
stop:
	docker-compose down

# Stop and DELETE all data (destructive!)
reset:
	@echo "⚠️  WARNING: This will DELETE all Redis data!"
	docker-compose down -v

# View logs
logs:
	docker-compose logs -f
