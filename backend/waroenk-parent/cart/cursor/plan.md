# Cart Service Plan — Spring Boot 3, Maven, MongoDB + Redis

> Complete implementation plan, schemas, endpoints, and operational notes. Use this as a blueprint to implement the Cart and Checkout microservice.

---

## 1. Overview

This plan describes a cart service built with Spring Boot 3 (Maven), using MongoDB and Redis as storage. The design separates transient, lock/hold behavior (checkout) from persisted snapshots (cart items). The service exposes controllers for cart operations, checkout workflow, system parameters, cache operations and version check.

Key collections:

* `cart_items` — canonical snapshot of user cart (persisted in MongoDB)
* `checkout_items` — validated/locked items for a short time (store in Redis; optional MongoDB persistence with TTL for audit)
* `system_parameters` — configurable system parameters

Primary services:

* `CartService`
* `CheckoutService`
* `SystemParameterService`

Primary controllers:

* `cart-controller`
* `checkout-controller`
* `system-parameter-controller`
* `version-controller`
* `cache-controller`

---

## 2. Technology & dependencies

* Java 17+ (or 21 if you prefer)
* Spring Boot 3.x
* Spring Data MongoDB
* Spring Data Redis (Lettuce recommended)
* Spring Web, Validation
* Lombok (optional)
* MapStruct (optional for DTO mapping)
* JUnit + Mockito (testing)

Partial `pom.xml` dependencies (add to `<dependencies>`):

```xml
<!-- Spring Boot starter -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- MongoDB -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>

<!-- Redis (Lettuce) -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Validation -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- Lombok (optional) -->
<dependency>
  <groupId>org.projectlombok</groupId>
  <artifactId>lombok</artifactId>
  <optional>true</optional>
</dependency>

<!-- Test -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-test</artifactId>
  <scope>test</scope>
</dependency>
```

---

## 3. `application.properties` (example)

```properties
spring.application.name=cart-service
server.port=8080

# Mongo
spring.data.mongodb.uri=mongodb://localhost:27017/cartdb
spring.data.mongodb.database=cartdb

# Redis (Lettuce)
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.timeout=2000ms

# Custom config
cart.checkout.ttl-seconds=900            # 15 minutes default TTL for checkout items
cart.checkout.use-redis=true             # store checkout in redis

# Logging, metrics, etc.
management.endpoints.web.exposure.include=health,info,prometheus
```

---

## 4. Data model & schemas

### 4.1 `cart_items` (MongoDB) — persisted snapshot

Document example (JSON):

```json
{
  "_id": "<cartId>" ,
  "userId": "user-123",
  "items": [
    { "sku": "SKU-001", "quantity": 2, "priceSnapshot": 12500, "title": "Product A" },
    { "sku": "SKU-002", "quantity": 1, "priceSnapshot": 4000, "title": "Product B" }
  ],
  "currency": "USD",
  "metadata": {
    "lastUpdatedBy": "user-123",
    "lastUpdatedAt": "2025-12-03T10:00:00Z"
  },
  "version": 3
}
```

Fields & notes:

* `_id`: cart id (e.g., `cart:userId` or UUID)
* `userId`: reference to user
* `items[]`: each element contains `sku`, `quantity`, snapshot fields like `priceSnapshot`, `title`, `attributes` (optional)
* `currency`: optional
* `version`: optimistic concurrency integer (increment on update)

Recommended indexes:

* `{ userId: 1 }` — find carts by user quickly
* `{ "items.sku": 1 }` — optional if you search inside items

Mongo index creation (shell):

```js
db.cart_items.createIndex({ userId: 1 }, { unique: true, name: 'idx_cart_user' })
db.cart_items.createIndex({ 'items.sku': 1 }, { name: 'idx_cart_items_sku' })
```

### 4.2 `checkout_items` — validated/locked items (Redis primary, Mongo optional)

**Primary storage: Redis** — store a serialized object keyed by `checkout:{checkoutId}` or `checkout:user:{userId}` with `EXPIRE` set to `cart.checkout.ttl-seconds`.

Value structure (JSON):

```json
{
  "checkoutId": "chk-123",
  "userId": "user-123",
  "items": [ { "sku": "SKU-001", "quantity": 2 }, { "sku": "SKU-002", "quantity": 1 } ],
  "lockedAt": "2025-12-03T10:05:00Z",
  "expiresAt": "2025-12-03T10:20:00Z",
  "sourceCartId": "cart-user-123"
}
```

Redis usage:

* Use `SET key value EX <ttl>` or `opsForValue().set(key, value, Duration.ofSeconds(ttl))`.
* Use Redis Hash if you need field-level ops (`HSET`) but JSON serialized string is simpler.
* To implement atomic inventory lock/unlock, use Redis Lua scripts or Redis Streams / Redlock for distributed locks.

**Optional MongoDB persistence** (audit): store same document in `checkout_items` collection with a TTL index on `expiresAt` so Mongo will auto remove when expired.

Mongo document example (same shape). Indexes for Mongo:

```js
db.checkout_items.createIndex({ checkoutId: 1 }, { unique: true })
db.checkout_items.createIndex({ expiresAt: 1 }, { expireAfterSeconds: 0 })
```

**Note:** TTL index in Mongo removes documents **at least** the TTL after expiry (background job); Redis expiration is precise and preferred for short-lived locks.

### 4.3 `system_parameters` collection (MongoDB)

Document example:

```json
{
  "_id": "cart.checkout.ttl-seconds",
  "key": "cart.checkout.ttl-seconds",
  "value": "900",
  "description": "TTL for checkout items in seconds",
  "type": "INTEGER",
  "updatedAt": "2025-12-03T09:00:00Z"
}
```

Indexes:

* `{ key: 1 }` unique

```js
db.system_parameters.createIndex({ key: 1 }, { unique: true })
```

---

## 5. Repositories

* Use Spring Data Mongo repositories for `CartItem` and `SystemParameter`.
* For `checkout_items`, implement a `CheckoutRepository` that writes to Redis (use `StringRedisTemplate` or `RedisTemplate<String, CheckoutDto>`). Also optionally write to Mongo for audit via `MongoTemplate`.

Example repository interfaces:

```java
public interface CartItemRepository extends MongoRepository<CartItem, String> {
    Optional<CartItem> findByUserId(String userId);
}

public interface SystemParameterRepository extends MongoRepository<SystemParameter, String> {
    Optional<SystemParameter> findByKey(String key);
}
```

For Redis: implement `CheckoutRepository` as a custom component that wraps `StringRedisTemplate`.

---

## 6. Services design

### 6.1 `CartService` responsibilities

* Add item to cart (snapshot price/title if available)
* Remove item
* Bulk add / bulk remove
* Update item quantity
* Get cart by user
* Persist cart to Mongo
* Maintain `version` for optimistic concurrency

Important implementation details:

* Use `findByUserId()`, apply modifications in memory, then `save()` with optimistic check on `version` (compare-and-set pattern). If concurrent modification, retry limited times.
* For heavy concurrency, consider Mongo `findOneAndUpdate` with `$set` and `$inc` to avoid race conditions.

### 6.2 `CheckoutService` responsibilities

* Validate cart against inventory & business rules (per-item inventory check via Inventory API)
* Reserve/lock quantities (call InventoryService or use a distributed lock mechanism)
* Create checkout token (checkoutId) and store in Redis with TTL
* Release locks when invalidated or TTL expires
* Provide endpoint to finalize (on successful order create) which will remove checkout entry and keep inventory reduced by final operation in Inventory service

Implementation notes:

* Validation flow:

    1. Load cart snapshot
    2. For each SKU, call Inventory API `checkAndReserve(sku, qty, checkoutId)` or use an atomic Redis decrement + fallback
    3. If all items reserved -> create Redis checkout key with TTL
    4. If any fail -> rollback previously reserved quantities
* Use distributed transactions? Prefer orchestration pattern — Inventory service supports reserve/release endpoints.
* Use Redis LUA scripts for atomic decrement/increment if inventory is in Redis.

### 6.3 `SystemParameterService`

* CRUD operations for `system_parameters` collection
* Provide cached reads (cache results in Redis) and invalidate cache when a parameter is updated
* Expose typed getters `getInt(key, defaultVal)` etc.

---

## 7. Controllers & endpoints

### 7.1 `cart-controller`

Base path: `/api/v1/cart`

* `POST /api/v1/cart` — create or upsert cart (body: { userId, items[] })
* `GET /api/v1/cart/{userId}` — get cart snapshot
* `POST /api/v1/cart/{userId}/add` — add single item
* `POST /api/v1/cart/{userId}/bulk-add` — add multiple items
* `POST /api/v1/cart/{userId}/remove` — remove single item
* `POST /api/v1/cart/{userId}/bulk-remove` — remove multiple SKUs
* `PUT /api/v1/cart/{userId}/update` — update item quantity (snapshot)

Behavior notes:

* All mutation endpoints update `lastUpdatedAt` and increment `version`.
* Validate input quantities (>= 0). If quantity == 0 remove item.

### 7.2 `checkout-controller`

Base path: `/api/v1/checkout`

* `POST /api/v1/checkout/validate` — call with `userId` or `cartId`; validates & reserves inventory, returns `checkoutId` and expiry
* `POST /api/v1/checkout/invalidate` — release locks and delete checkout entry
* `GET /api/v1/checkout/{checkoutId}` — get checkout snapshot
* `POST /api/v1/checkout/finalize` — used by order service after payment to finalize and remove checkout and finalize inventory changes

Security:

* Ensure caller is authorized for userId

### 7.3 `system-parameter-controller`

Base path: `/api/v1/system-parameters`

* `GET /api/v1/system-parameters` — list
* `GET /api/v1/system-parameters/{key}`
* `POST /api/v1/system-parameters` — create
* `PUT /api/v1/system-parameters/{key}` — update
* `DELETE /api/v1/system-parameters/{key}`

### 7.4 `version-controller`

* `GET /api/v1/version` — return application name, version, build time

### 7.5 `cache-controller`

* `POST /api/v1/cache/flush-all` — flush all redis keys (CAUTION)
* `POST /api/v1/cache/flush` — body: `{ "pattern": "cart:*" }` — deletes matching keys (use SCAN + DEL, avoid KEYS in production)

---

## 8. Concurrency and inventory locking strategies

Options:

1. **Inventory service with reserve/release endpoints** (recommended): orchestrate calls and rely on Inventory to be authoritative. Provide idempotency keys.
2. **Redis-based reservation**: maintain inventory counts in Redis and use Lua scripts that atomically decrement and set rollback on failure.
3. **Mongo optimistic locking**: for cart updates only; inventory locking must be separate.

Atomic reservation example (pseudo-Lua): decrement each SKU stock only if `stock >= qty`, otherwise rollback previous decrements.

---

## 9. Error handling & retries

* Validate inputs and return `400` on invalid payloads.
* `409 Conflict` for optimistic lock failures.
* Retry small number (3) for transient DB/network issues.
* Compensating rollback when any reservation fails.

---

## 10. Observability & metrics

* Expose Prometheus metrics (Micrometer) for request rates, failures, checkout success/fail ratios
* Logs: structured JSON logs including correlationId and userId
* Traces: OpenTelemetry / Zipkin to trace calls to Inventory and Order services

---

## 11. Testing

* Unit tests for service logic (mock repositories and Inventory client)
* Integration tests with embedded Mongo and Embedded Redis (or Testcontainers)
* Contract tests for Inventory service interactions

---

## 12. Sample code skeletons (signatures only)

`CartService` interface (Java):

```java
public interface CartService {
  CartDto getCart(String userId);
  CartDto addItem(String userId, CartItemDto item);
  CartDto bulkAdd(String userId, List<CartItemDto> items);
  CartDto removeItem(String userId, String sku);
  CartDto bulkRemove(String userId, List<String> skus);
  CartDto updateItem(String userId, CartItemDto item);
}
```

`CheckoutService` interface:

```java
public interface CheckoutService {
  CheckoutResult validateAndReserve(String userId);
  void invalidateCheckout(String checkoutId);
  CheckoutDto getCheckout(String checkoutId);
  void finalizeCheckout(String checkoutId);
}
```

`SystemParameterService` interface:

```java
public interface SystemParameterService {
  SystemParameterDto get(String key);
  SystemParameterDto put(SystemParameterDto dto);
  void delete(String key);
}
```

---

## 13. Deployment & ops notes

* Use Kubernetes with readiness/liveness probes
* Backups: MongoDB backup schedule and Redis persistence (RDB/AOF) depending on the use of Redis for critical data (checkout only — Redis persistence is optional since Mongo audit exists)
* Capacity: size Redis key count for checkout concurrency; monitor TTLs

---

## 14. Migration & backwards compatibility

* Keep schema versions in documents when evolving fields
* Provide migration scripts using `mongock` or `flyway`-like tool for Mongo

---

## 15. Useful Mongo & Redis commands

Mongo indexes:

```js
use cartdb
db.cart_items.createIndex({ userId: 1 }, { unique: true })
db.checkout_items.createIndex({ expiresAt: 1 }, { expireAfterSeconds: 0 })
db.system_parameters.createIndex({ key: 1 }, { unique: true })
```

Redis: set checkout with TTL:

```
SET checkout:user:123 "{...json...}" EX 900
```

Scan-delete pattern (safe):

```
SCAN 0 MATCH checkout:* COUNT 1000
DEL <keys...>
```

---

## 16. Next steps / Implementation roadmap

1. Initialize Maven project + dependencies
2. Create domain models, DTOs and repositories
3. Implement CartService + unit tests
4. Implement Redis-backed CheckoutRepository and CheckoutService wiring to Inventory stub
5. Implement controllers and validation
6. Add system-parameter CRUD and caching
7. Integration tests with testcontainers (Mongo, Redis)
8. Add metrics, tracing, logging
9. Kubernetes manifests and CI pipeline

---

*End of plan.*
