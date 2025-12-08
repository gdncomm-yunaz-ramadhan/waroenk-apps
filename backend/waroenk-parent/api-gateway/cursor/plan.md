# Dynamic API Gateway Implementation (Spring Boot 3 + gRPC)

## 1. Overview

An **agnostic, dynamic API Gateway** using **Spring Boot 3** that:

* Accepts HTTP/HTTPS requests from clients (REST)
* Routes requests to microservices via **gRPC**
* Performs **JWT-based authentication and authorization**
* **Dynamically registers services** at runtime via gRPC
* Uses **PostgreSQL** for persistent route storage
* Uses **Redis** for fast route caching
* Handles **error translation** (gRPC → HTTP)

**Key Feature: No code changes needed to add new microservices!**

---

## 2. Architecture

```
HTTP Client (Svelte UI)
        |
        v
   API GATEWAY (Spring Boot)
 ┌───────────────────────────────────────────────┐
 | HTTP REST API (:8080)                         |
 |   - JWT Validation                            |
 |   - Route Resolution                          |
 |   - JSON ↔ Proto Translation                  |
 | ─────────────────────────────────────────────|
 | gRPC Registration Server (:6565)              |
 |   - RegisterService RPC                       |
 |   - Heartbeat RPC                             |
 |   - CheckRoutes RPC (smart registration)      |
 | ─────────────────────────────────────────────|
 | Storage Layer                                 |
 |   - PostgreSQL (persistent routes)            |
 |   - Redis (route cache)                       |
 └───────────────────────────────────────────────┘
        |
        v
  gRPC Microservices
 ┌─────────┐ ┌─────────┐ ┌─────────┐
 │ Member  │ │ Catalog │ │  Cart   │
 └─────────┘ └─────────┘ └─────────┘
```

---

## 3. Service Registration

### 3.1 Registration Flow

```
Microservice Startup
        |
        v
  ┌─────────────────────────────────┐
  │ 1. Check which routes changed   │
  │    (CheckRoutes RPC)            │
  └─────────────────────────────────┘
        |
        v
  ┌─────────────────────────────────┐
  │ 2. Register only new/changed    │
  │    routes (RegisterService RPC) │
  └─────────────────────────────────┘
        |
        v
  ┌─────────────────────────────────┐
  │ 3. Start heartbeat (every 30s) │
  │    (Heartbeat RPC)              │
  └─────────────────────────────────┘
```

### 3.2 Smart Registration (Idempotent)

Routes include a hash computed from:
- HTTP method + path + gRPC service + method + request/response types

If a route's hash matches an existing route, it's skipped (no update needed).

### 3.3 Non-Blocking Registration

- Registration runs **asynchronously** in a background thread
- **Won't block** microservice startup
- Failures are logged but **ignored** (microservice continues to work)
- Automatic retry with backoff

---

## 4. Project Structure

```
api-gateway/
├── src/main/java/com/gdn/project/waroenk/gateway/
│   ├── ApiGatewayApplication.java
│   ├── config/
│   │   ├── GatewayProperties.java      # Static route config (fallback)
│   │   ├── GrpcChannelConfig.java      # gRPC channel management
│   │   ├── SecurityConfig.java         # JWT + Spring Security
│   │   ├── SchedulingConfig.java       # Heartbeat cleanup tasks
│   │   ├── RedisConfig.java            # Redis template
│   │   └── ...
│   ├── controller/
│   │   ├── GatewayController.java      # Dynamic /api/** routing
│   │   ├── HealthController.java       # /health, /info, /routes, /services
│   │   └── ControllerAdvice.java       # Global exception handler
│   ├── grpc/
│   │   └── GatewayRegistrationServiceImpl.java  # gRPC registration server
│   ├── service/
│   │   ├── DynamicRoutingRegistry.java # Route persistence + caching
│   │   ├── RouteResolver.java          # Route resolution (static + dynamic)
│   │   ├── GrpcProxyService.java       # Dynamic gRPC invocation
│   │   └── GrpcServiceRegistry.java    # Proto message type registry
│   ├── entity/
│   │   ├── ServiceRegistryEntity.java
│   │   └── RouteRegistryEntity.java
│   ├── repository/
│   │   ├── ServiceRegistryRepository.java
│   │   └── RouteRegistryRepository.java
│   └── ...
├── src/main/resources/
│   ├── application.properties
│   └── db/migration/
│       └── V1__init_gateway_schema.sql
├── Dockerfile
├── docker-compose.yml  # Includes PostgreSQL + Redis
└── Makefile
```

---

## 5. Client Registration (Microservice Side)

### 5.1 Using GatewayRegistrationClient

```java
@Component
public class GatewayRegistration {
    
    @Value("${gateway.host:localhost}")
    private String gatewayHost;
    
    @Value("${gateway.grpc.port:6565}")
    private int gatewayPort;
    
    @Value("${server.grpc.port:9090}")
    private int servicePort;
    
    @PostConstruct
    public void register() {
        // Async, non-blocking - won't affect startup
        new GatewayRegistrationClient.Builder(gatewayHost, gatewayPort)
            .service("member", "member", servicePort)
            .addRoute("POST", "/api/user/register", 
                "member.user.UserService", "Register",
                "com.gdn.project.waroenk.member.CreateUserRequest",
                "com.gdn.project.waroenk.member.CreateUserResponse", 
                true)  // public endpoint
            .addRoute("POST", "/api/user/login",
                "member.user.UserService", "Authenticate",
                "com.gdn.project.waroenk.member.AuthenticateRequest",
                "com.gdn.project.waroenk.member.UserTokenResponse",
                true)
            .addRoute("GET", "/api/user",
                "member.user.UserService", "GetOneUserById",
                "com.gdn.project.waroenk.common.Id",
                "com.gdn.project.waroenk.member.UserData",
                false)  // protected endpoint
            .registerAsync()
            .thenAccept(success -> {
                if (success) {
                    log.info("Registered with gateway successfully");
                }
            });
    }
}
```

### 5.2 Features

- **Async/Non-blocking**: Uses `CompletableFuture`
- **Automatic heartbeat**: Keeps service active in gateway
- **Smart registration**: Only registers changed routes
- **Failure tolerant**: Logs errors but doesn't crash
- **Auto-retry**: Retries registration on failure

---

## 6. Configuration

### 6.1 Gateway application.properties

```properties
# Server ports
server.port=8080
grpc.server.port=6565

# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/gatewaydb
spring.datasource.username=admin
spring.datasource.password=admin

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Static services (fallback if not registered)
gateway.services.member.host=localhost
gateway.services.member.port=9090

gateway.services.catalog.host=localhost
gateway.services.catalog.port=9091
```

### 6.2 Microservice Configuration

```properties
# Gateway registration
gateway.host=api-gateway
gateway.grpc.port=6565

# This service
server.grpc.port=9090
spring.application.name=member
```

---

## 7. Database Schema

### service_registry

| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| name | VARCHAR(200) | Unique service name |
| host | VARCHAR(200) | Service host |
| port | INT | gRPC port |
| active | BOOLEAN | Is service active |
| last_heartbeat | TIMESTAMP | Last heartbeat time |
| created_at | TIMESTAMP | Registration time |

### route_registry

| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| service_id | UUID | FK to service_registry |
| http_method | VARCHAR(20) | GET/POST/PUT/DELETE |
| path | VARCHAR(500) | HTTP path pattern |
| grpc_service | VARCHAR(500) | gRPC service name |
| grpc_method | VARCHAR(500) | gRPC method name |
| request_type | VARCHAR(500) | Proto request class |
| response_type | VARCHAR(500) | Proto response class |
| public_endpoint | BOOLEAN | Auth required? |
| route_hash | VARCHAR(64) | SHA-256 for change detection |

---

## 8. Health Monitoring

### 8.1 Heartbeat Mechanism

- Services send heartbeat every 30 seconds
- Gateway marks services inactive after 2 missed heartbeats
- Scheduled cleanup removes stale routes from cache

### 8.2 Endpoints

| Endpoint | Description |
|----------|-------------|
| `/health` | Gateway health status |
| `/info` | Gateway info + route/service counts |
| `/services` | List all registered services |
| `/routes` | List all routes (static + dynamic) |
| `/routes/summary` | Routes grouped by service |

---

## 9. Running

### Development

```bash
cd api-gateway

# Start infrastructure (PostgreSQL + Redis)
make infra

# Compile and run locally
make compile
cd .. && ./mvnw spring-boot:run -pl api-gateway
```

### Docker

```bash
cd api-gateway

# Build and start everything
make all

# View logs
make logs

# Check status
make status
make routes
make services
```

### Ports

| Port | Service |
|------|---------|
| 8080 | HTTP REST API |
| 6565 | gRPC Registration |
| 5433 | PostgreSQL (gateway-db) |
| 6380 | Redis (gateway-redis) |

---

## 10. Error Handling

### gRPC → HTTP Status Mapping

| gRPC Status | HTTP Status | Error Code |
|-------------|-------------|------------|
| NOT_FOUND | 404 | RESOURCE_NOT_FOUND |
| ALREADY_EXISTS | 409 | RESOURCE_EXISTS |
| INVALID_ARGUMENT | 400 | INVALID_ARGUMENT |
| UNAUTHENTICATED | 401 | UNAUTHENTICATED |
| PERMISSION_DENIED | 403 | PERMISSION_DENIED |
| UNAVAILABLE | 503 | SERVICE_UNAVAILABLE |
| INTERNAL | 500 | INTERNAL_ERROR |

### Error Response Format

```json
{
  "status": 404,
  "message": "NOT_FOUND",
  "details": "No microservice registered for GET /api/orders",
  "timestamp": "2024-01-15T10:30:00"
}
```

---

## 11. Future Enhancements

- [ ] Service load balancing (round-robin, weighted)
- [ ] Circuit breaker (Resilience4j)
- [ ] Rate limiting per client/service
- [ ] Request/response caching
- [ ] gRPC streaming support
- [ ] OpenTelemetry tracing
- [ ] Dynamic route reload without restart
