.PHONY: up down stop reset logs shell tools status network

# Create shared network if not exists
network:
	@docker network inspect waroenk-network >/dev/null 2>&1 || docker network create waroenk-network
	@echo "✅ Network waroenk-network ready"

# Start MongoDB only
up: network
	docker-compose up -d
	@echo "✅ MongoDB started on localhost:27017"
	@echo "   User: admin, Password: admin"
	@echo "   Max Connections: 500"

# Start MongoDB + Mongo Express GUI
tools:
	docker-compose --profile tools up -d
	@echo "✅ MongoDB started on localhost:27017"
	@echo "✅ Mongo Express started on http://localhost:8082"

# Stop MongoDB (keeps data)
down:
	docker-compose --profile tools down

# Alias for down
stop:
	docker-compose --profile tools down

# Stop and DELETE all data (destructive!)
reset:
	@echo "⚠️  WARNING: This will DELETE all MongoDB data!"
	@read -p "Are you sure? [y/N] " confirm && [ "$$confirm" = "y" ] || exit 1
	docker-compose --profile tools down -v
	@echo "✅ All data deleted"

# View logs
logs:
	docker-compose logs -f mongodb

# Connect to MongoDB shell
shell:
	docker exec -it mongodb mongosh -u admin -p admin --authenticationDatabase admin catalog

# Check status
status:
	@docker-compose ps
	@echo ""
	@echo "Collections in catalog database:"
	@docker exec -it mongodb mongosh -u admin -p admin --authenticationDatabase admin catalog --quiet --eval "db.getCollectionNames()" 2>/dev/null || echo "Container not running"
