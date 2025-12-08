# API Gateway

Agnostic API Gateway that routes HTTP requests to gRPC microservices.

## Quick Start

```bash
# Build and run
make all

# Or step by step:
make build    # Build Docker image
make up       # Start service
make logs     # View logs
```

## Endpoints

| Endpoint | Description |
|----------|-------------|
| http://localhost:8080/health | Health check |
| http://localhost:8080/info | Gateway info |
| http://localhost:8080/routes | List routes |
| http://localhost:8080/swagger-ui.html | API docs |

## Configuration

All routing is configured in `application.properties`. See [cursor/plan.md](cursor/plan.md) for details.

## Environment Variables

Copy `.env.example` to `.env` and modify:

```
HTTP_REST_PORT=8080
MEMBER_GRPC_HOST=member
MEMBER_GRPC_PORT=9090
CATALOG_GRPC_HOST=catalog
CATALOG_GRPC_PORT=9091
```










