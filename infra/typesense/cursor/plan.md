# 📌 Typesense Optimization & Resilience Plan

This document provides a structured plan to improve the stability, resilience, and performance of Typesense in a containerized environment and optimize its Java (JVM) client configuration.

---

## ✔️ Phase 1 — Improve Docker Compose Configuration

### **1. Use Persistent Storage & Correct Permissions**

* Ensure a persistent host volume:

  ```yaml
  volumes:
    - ./typesense-data:/data
  ```
* Fix ownership (if needed):

  ```bash
  sudo chown -R 10001:10001 ./typesense-data
  ```

---

### **2. Apply Recommended Environment Variables**

* Force Typesense to listen correctly in Docker.
* Enable CORS if accessing from another service/frontend.

```yaml
environment:
  TYPESENSE_API_KEY: "YOUR_ADMIN_KEY"
  TYPESENSE_DATA_DIR: "/data"
  TYPESENSE_LISTEN_ADDRESS: "0.0.0.0"
  TYPESENSE_ENABLE_CORS: "true"
  TYPESENSE_LOG_LEVEL: "info" # change to 'debug' for deep troubleshooting
```

---

### **3. Adjust Health Check**

* Prevent Docker marking Typesense as unhealthy during cold start or indexing.

```yaml
typesense:
  healthcheck:
    test: ["CMD-SHELL", "curl -fs http://localhost:8108/health?api_key=YOUR_ADMIN_KEY || exit 1"]
    interval: 20s
    timeout: 5s
    retries: 5
    start_period: 30s
```

---

### **4. Resource Allocation (Recommended)**

Set minimum resource guarantees to avoid OOM kills.

```yaml
deploy:
  resources:
    limits:
      memory: 2g
    reservations:
      memory: 1g
```

---

### **5. Optional — Dedicated Network with Static Naming**

```yaml
networks:
  typesense-net:
    driver: bridge
```

---

## ✔️ Phase 2 — JVM Client Configuration Improvements

### **1. Connection Pooling & Reuse**

Avoid creating a client per request. Use a singleton.

```java
ClientConfig config = new ClientConfig("http://typesense:8108", "YOUR_ADMIN_KEY")
    .withConnectionTimeout(Duration.ofSeconds(3))
    .withReadTimeout(Duration.ofSeconds(5))
    .withWriteTimeout(Duration.ofSeconds(5))
    .withMaxConnections(50)              // prevent connection flood
    .withRetries(3)                      // retry failed requests
    .withRetryInterval(Duration.ofMillis(300));

TypesenseClient client = new Client(config);
```

---

### **2. Enable Verbose Logging (Client Side)**

```java
System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
```

Or using Spring Boot:

```yaml
logging.level.org.typesense: DEBUG
logging.level.com.squareup.okhttp3: DEBUG
```

---

### **3. Add Circuit Breaker (Optional for Microservices)**

Use Resilience4J or Spring Cloud Circuit Breaker to avoid cascading failures.

```yaml
resilience4j.circuitbreaker:
  typesense:
    slidingWindowSize: 10
    minimumNumberOfCalls: 5
    failureRateThreshold: 50
    waitDurationInOpenState: 30s
```

---

## ✔️ Phase 3 — Operational Monitoring & Health Metrics

### Enable Runtime Metrics

Run:

```bash
docker exec -it typesense curl http://localhost:8108/stats.json?api_key=YOUR_ADMIN_KEY
```

Monitor:

* Query latency
* Indexing time
* CPU usage
* Memory usage

---

## 🔧 Optional Enhancements

| Feature                 | Purpose                | Priority             |
| ----------------------- | ---------------------- | -------------------- |
| Enable TLS              | Secure external access | Medium               |
| HA Cluster Mode         | Failure tolerance      | Low (unless scaling) |
| Nightly snapshot script | Disaster recovery      | Medium               |

---

## Final Notes

* Always run indexing in batches.
* Avoid bulk rebuilds during high load.
* Review resource stats monthly.

---

### 🚀 Result

By applying this plan, Typesense should become:

* Stable
* Responsive
* Observable
* Safe from sudden crashes or random timeouts.

---

**Next Step:** Run diagnostics again after applying changes to verify stability.

---
