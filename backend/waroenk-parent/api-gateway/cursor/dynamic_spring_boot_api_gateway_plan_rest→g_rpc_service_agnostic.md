# Dynamic Spring Boot API Gateway — gRPC Registration + REST↔gRPC Translation

This plan describes how to build a **service-agnostic, dynamic API Gateway** in Spring Boot 3 that communicates via **gRPC internally** and supports **runtime service registration** via **gRPC**, rather than HTTP. The gateway exposes REST endpoints to clients (e.g., Svelte UI) and dynamically translates requests into gRPC calls.

---

## 🧩 Overview

### Architecture
```
HTTP Client (Svelte UI)
        |
        v
   API GATEWAY (Spring Boot)
 ┌───────────────────────────────────────────────┐
 | JWT Validation + Session Check               |
 | Dynamic Service Registry (Redis/Postgres)    |
 | gRPC Registration Service (for microservices)|
 | JSON↔Proto Mapper (DynamicMessage)           |
 | Routing Engine (path→gRPC method resolver)   |
 └───────────────────────────────────────────────┘
        |
        v
  gRPC Microservices (Spring Boot, Go, etc)
```

---

## ⚙️ Responsibilities

### API Gateway
- Validate JWT session for all REST requests
- Maintain dynamic routing registry (service + method mapping)
- Receive **gRPC registration requests** from microservices
- Auto-load descriptors via **gRPC reflection or uploaded FileDescriptorSet**
- Translate incoming REST→gRPC calls
- Return JSON-formatted HTTP responses (from gRPC)
- Gracefully remove unresponsive services (health checks)

### Microservices
- Expose gRPC endpoints normally
- On startup, call **`GatewayRegistrationService.RegisterService`** via gRPC to announce themselves
- Optionally expose reflection API or upload their descriptor

---

## 🧭 gRPC Contract

### 1. `gateway.proto`
```proto
tsyntax = "proto3";

package gateway;

service GatewayRegistrationService {
  rpc RegisterService (ServiceDefinition) returns (RegistrationAck);
  rpc Heartbeat (ServicePing) returns (HeartbeatAck);
}

message ServiceDefinition {
  string name = 1;
  string protocol = 2; // e.g., grpc
  string host = 3;
  int32 port = 4;
  string descriptorUrl = 5; // Optional (if reflection unsupported)
  repeated RouteDefinition routes = 6;
}

message RouteDefinition {
  string httpMethod = 1;
  string path = 2;
  string grpcService = 3;
  string grpcMethod = 4;
}

message RegistrationAck {
  bool success = 1;
  string message = 2;
}

message ServicePing {
  string serviceName = 1;
}

message HeartbeatAck {
  bool active = 1;
}
```

Microservices call `RegisterService()` at startup to announce their routes.

---

## 🧱 Gateway Internal Modules

| Module | Description |
|--------|-------------|
| **GatewayApplication** | Spring Boot main entrypoint |
| **JwtAuthFilter** | Extracts and validates JWTs from incoming REST requests |
| **GrpcRegistrationServer** | Exposes the `GatewayRegistrationService` for microservices to register dynamically |
| **RoutingRegistry** | Stores service mappings in Redis or PostgreSQL |
| **DescriptorLoader** | Loads and caches service descriptors (via reflection or provided descriptor URL) |
| **RestToGrpcTranslator** | Converts JSON payloads to `DynamicMessage` gRPC requests |
| **GrpcInvoker** | Uses `ManagedChannel` + `MethodDescriptor` to execute gRPC calls dynamically |
| **ResponseMapper** | Converts `DynamicMessage` responses to JSON for REST clients |

---

## 💾 Database Choice

| Storage | Use Case | Why |
|----------|-----------|------|
| **Redis** | Fast registration + heartbeat tracking | Excellent for caching active services |
| **PostgreSQL** | Persistent route + metadata storage | Ideal if you need service history or manual query |

### Recommendation
Use **Redis for runtime registry**, and **PostgreSQL for persistent fallback**.

---

## 🧠 Data Model (PostgreSQL) + Flyway Migrations

### Table: `service_registry`
| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Primary key |
| name | VARCHAR | Service name |
| host | VARCHAR | Host address |
| port | INT | gRPC port |
| descriptor_url | TEXT | URL to download `.desc` file |
| created_at | TIMESTAMP | Registration time |

### Table: `route_registry`
| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Primary key |
| service_id | UUID | FK to service_registry |
| http_method | VARCHAR | GET/POST/PUT/etc |
| path | VARCHAR | REST path pattern |
| grpc_service | VARCHAR | gRPC service name |
| grpc_method | VARCHAR | gRPC method name |

### 📦 Flyway Migration

Use Flyway to auto-create the DB structure.

Database settings:
- **DB Name:** `gatewaydb`
- **Username:** `admin`
- **Password:** `admin`

#### Example `application.properties`
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/gatewaydb
spring.datasource.username=admin
spring.datasource.password=admin
spring.jpa.hibernate.ddl-auto=none
spring.flyway.enabled=true
```

#### Flyway Migration File: `V1__init_gateway_schema.sql`
```sql
CREATE TABLE service_registry (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE,
    host VARCHAR(200) NOT NULL,
    port INT NOT NULL,
    descriptor_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_service_registry_name ON service_registry(name);
CREATE INDEX idx_service_registry_host_port ON service_registry(host, port);


CREATE TABLE route_registry (
    id UUID PRIMARY KEY,
    service_id UUID NOT NULL REFERENCES service_registry(id) ON DELETE CASCADE,
    http_method VARCHAR(20) NOT NULL,
    path VARCHAR(500) NOT NULL,
    grpc_service VARCHAR(500) NOT NULL,
    grpc_method VARCHAR(500) NOT NULL
);

CREATE UNIQUE INDEX idx_route_unique ON route_registry(http_method, path);
CREATE INDEX idx_route_service ON route_registry(service_id);
```

---

## 🧩 REST → gRPC Flow

1. **JWT Validation** → `JwtAuthFilter`
2. **Routing Match** → Match REST path/method to `RouteDefinition`
3. **Descriptor Lookup** → Find descriptor from cache (Redis / in-memory)
4. **Message Translation** → JSON → `DynamicMessage`
5. **Invoke** → Execute via `GrpcInvoker`
6. **Map Response** → `DynamicMessage` → JSON

```java
DynamicMessage grpcReq = JsonFormat.parser()
  .ignoringUnknownFields()
  .merge(jsonBody, DynamicMessage.newBuilder(inputDesc));

DynamicMessage resp = blockingStub.invoke(grpcReq);
String json = JsonFormat.printer().print(resp);
```

---

## 🔍 Validation Rules

- Registration must contain:
  - Unique service name
  - Valid host:port
  - At least one route
- Gateway validates:
  - Host connectivity
  - Duplicate routes conflict
  - Optional descriptor fetch (if `descriptorUrl` provided)
- During invocation:
  - JWT validity
  - Route existence
  - Service availability (ping check)

---

## 🔄 Auto Unregister / Health Check

- Each registered microservice must send a `Heartbeat` gRPC call every 30s.
- If missed > 2 cycles, mark as inactive and remove routes.
- Redis TTL handles automatic expiration.

---

## 🧰 Technologies

| Component | Tech |
|------------|------|
| Framework | Spring Boot 3 (WebFlux + gRPC Netty) |
| Serialization | protobuf-java, google-json-format |
| Cache | Redis (Lettuce) |
| Database | PostgreSQL (JPA/Hibernate) |
| Auth | JWT via `spring-security-oauth2-jose` |
| Testing | Testcontainers + gRPC in-memory server |

---

## 🚀 Deployment

- Each microservice connects to Gateway via gRPC channel:
  ```java
  ManagedChannel channel = ManagedChannelBuilder.forAddress(gatewayHost, gatewayPort).usePlaintext().build();
  GatewayRegistrationServiceGrpc.GatewayRegistrationServiceBlockingStub stub = GatewayRegistrationServiceGrpc.newBlockingStub(channel);
  stub.registerService(definition);
  ```
- Gateway listens on:
  - **Port 8080 (HTTP)** for REST clients
  - **Port 6565 (gRPC)** for service registration

---

## 🔮 Future Improvements
- Service load balancing
- Per-route authentication scopes
- Rate limiting per microservice
- Circuit breaker with retry

---

## 🚫 Route Not Found Behavior (Chosen: Option 3 — Structured Error)

When a client calls a path that has no matching registered route, the gateway will return a structured JSON error response. This avoids ambiguous HTTP-only responses and gives clients consistent machine-readable error payloads.

### Error JSON format
```json
{
  "error": "ROUTE_NOT_FOUND",
  "message": "No microservice registered for GET /orders",
  "path": "/orders",
  "method": "GET",
  "timestamp": 1735992612000
}
```

Fields:
- `error`: machine-readable error code
- `message`: human-friendly message
- `path`: requested HTTP path
- `method`: HTTP method used
- `timestamp`: epoch millis when the error occurred

### Implementation (Spring Boot Global Exception Handler)

Create a global `@ControllerAdvice` to handle `RouteNotFoundException` (custom) and other errors:

```java
@Data
@AllArgsConstructor
public class ErrorResponse {
    private String error;
    private String message;
    private String path;
    private String method;
    private long timestamp;
}

@ResponseStatus(HttpStatus.NOT_FOUND)
public class RouteNotFoundException extends RuntimeException {
    public RouteNotFoundException(String message) { super(message); }
}

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RouteNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRouteNotFound(RouteNotFoundException ex, HttpServletRequest req) {
        ErrorResponse res = new ErrorResponse(
            "ROUTE_NOT_FOUND",
            ex.getMessage(),
            req.getRequestURI(),
            req.getMethod(),
            Instant.now().toEpochMilli()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        ErrorResponse res = new ErrorResponse(
            "INTERNAL_ERROR",
            ex.getMessage(),
            req.getRequestURI(),
            req.getMethod(),
            Instant.now().toEpochMilli()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
    }
}
```

### Where to throw `RouteNotFoundException`

In the dynamic REST handler/controller where you match the incoming request against the in-memory routing table. If no route matches, throw `RouteNotFoundException` with message like: `"No microservice registered for GET /orders"`.

```java
Route route = routingService.match(method, path);
if (route == null) {
    throw new RouteNotFoundException("No microservice registered for " + method + " " + path);
}
```

### HTTP status mapping for gRPC errors

Map common gRPC status codes to HTTP codes and use the structured error body for non-200 results. Example mapping:

| gRPC Status | HTTP Status | Error Code |
|-------------|-------------|------------|
| OK | 200 | - |
| NOT_FOUND | 404 | RESOURCE_NOT_FOUND |
| UNAUTHENTICATED | 401 | UNAUTHENTICATED |
| PERMISSION_DENIED | 403 | PERMISSION_DENIED |
| INVALID_ARGUMENT | 400 | INVALID_ARGUMENT |
| INTERNAL | 500 | INTERNAL_ERROR |

When translating a gRPC error, throw an exception that the global handler will convert to the structured JSON payload.

---

## ✅ Summary
This approach ensures:
- ✅ Full dynamic service registration via gRPC
- ✅ REST clients stay protocol-agnostic
- ✅ JWT validation centralized at gateway
- ✅ Redis for runtime discovery, Postgres for persistence
- ✅ No Consul/Kubernetes dependency
- ✅ Clear structured error responses (ROUTE_NOT_FOUND) for missing routes

The result: a **self-managing, service-agnostic API gateway** that bridges REST clients and gRPC microservices seamlessly.

