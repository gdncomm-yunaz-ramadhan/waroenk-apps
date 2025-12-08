.PHONY: up down stop reset logs status

# Start Typesense
up:
	docker-compose up -d
	@echo "✅ Typesense started on localhost:8108"
	@echo "   API Key: $(TYPESENSE_API_KEY)"

# Stop Typesense (keeps data)
down:
	docker-compose down

# Alias for down
stop:
	docker-compose down

# Stop and DELETE all data (destructive!)
reset:
	@echo "⚠️  WARNING: This will DELETE all Typesense data!"
	@read -p "Are you sure? [y/N] " confirm && [ "$$confirm" = "y" ] || exit 1
	docker-compose down -v
	@echo "✅ All data deleted"

# View logs
logs:
	docker-compose logs -f

# Check status
status:
	@docker-compose ps
	@echo ""
	@echo "Health check:"
	@curl -s http://localhost:8108/health 2>/dev/null || echo "Container not running"
