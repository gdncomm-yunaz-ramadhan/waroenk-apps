.PHONY: up down stop reset logs shell tools network

# Create shared network if not exists
network:
	@docker network inspect waroenk-network >/dev/null 2>&1 || docker network create waroenk-network
	@echo "✅ Network waroenk-network ready"

# Start PostgreSQL only
up: network
	docker-compose up -d
	@echo "✅ PostgreSQL started on localhost:5432"
	@echo "   User: admin, Password: admin"
	@echo "   Max Connections: 200"

# Start PostgreSQL + pgAdmin
tools:
	docker-compose --profile tools up -d
	@echo "✅ PostgreSQL started on localhost:5432"
	@echo "✅ pgAdmin started on http://localhost:5050"
	@echo "   pgAdmin login: admin@admin.com / admin"

# Stop PostgreSQL (keeps data)
down:
	docker-compose --profile tools down

# Alias for down
stop:
	docker-compose --profile tools down

# Stop and DELETE all data (destructive!)
reset:
	@echo "⚠️  WARNING: This will DELETE all PostgreSQL data!"
	@read -p "Are you sure? [y/N] " confirm && [ "$$confirm" = "y" ] || exit 1
	docker-compose --profile tools down -v
	@echo "✅ All data deleted"

# View logs
logs:
	docker-compose logs -f postgres

# Connect to PostgreSQL shell
shell:
	docker exec -it postgres psql -U admin -d userdb

# Check status
status:
	@docker-compose ps
	@echo ""
	@echo "Databases:"
	@docker exec -it postgres psql -U admin -c "\l" 2>/dev/null || echo "Container not running"
