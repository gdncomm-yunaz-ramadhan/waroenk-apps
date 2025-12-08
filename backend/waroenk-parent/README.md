# Waroenk Parent - Multi-Module Maven Project

A multi-module Maven project for the Waroenk e-commerce platform with Docker support.

## Project Structure

```
waroenk-parent/
├── pom.xml                 # Parent POM
├── Dockerfile              # Multi-module Docker build
├── docker-compose.yml      # Orchestrates all services
├── Makefile                # Build automation
│
├── grpc-contract/          # Shared gRPC proto definitions
│   ├── pom.xml
│   └── src/main/proto/     # Proto files
│
├── member/                 # Member service
│   ├── pom.xml
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── Makefile
│   └── src/
│
├── catalog/                # Catalog service
│   ├── pom.xml
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── Makefile
│   └── src/
│
├── cart/                   # Cart service
│   ├── pom.xml
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── Makefile
│   └── src/
│
└── api-gateway/            # API Gateway
    ├── pom.xml
    ├── Dockerfile
    ├── docker-compose.yml
    ├── Makefile
    └── src/
```

## Build Strategy

### The Challenge

In a multi-module Maven project, the `grpc-contract` module is a shared dependency. When building with Docker:
- Locally: `mvn install` from parent installs `grpc-contract` to the local `.m2` repository
- Docker: Each container has an isolated filesystem

### The Solution

We use **Docker BuildKit cache mounts** and build from the **parent context**:

1. **Each module's Dockerfile** uses `context: ..` (parent directory)
2. **Build order**: First build and install `grpc-contract`, then the target module
3. **Cache mounts**: `--mount=type=cache,target=/root/.m2/repository` shares Maven cache across builds

```dockerfile
# From member/Dockerfile
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Copy parent pom and both module poms
COPY pom.xml .
COPY grpc-contract/pom.xml grpc-contract/
COPY member/pom.xml member/

# Build grpc-contract first and install to local repo
COPY grpc-contract/src grpc-contract/src
RUN --mount=type=cache,target=/root/.m2/repository \
    ./mvnw -B -DskipTests install -pl grpc-contract

# Now build member (can find grpc-contract in cache)
COPY member/src member/src
RUN --mount=type=cache,target=/root/.m2/repository \
    ./mvnw -B -DskipTests package -pl member
```

## Usage

### Option 1: Build & Run All Services (From Parent)

```bash
cd waroenk-parent

# Build all services
make build

# Start all services with infrastructure
make all

# Or step by step
make infra         # Start PostgreSQL + Redis
make build         # Build all Docker images
make up            # Start all services
```

### Option 2: Build & Run Individual Modules

Each module can be built and run independently:

```bash
cd waroenk-parent/member

# Build and run member service
make all

# Or step by step
make infra         # Start PostgreSQL + Redis
make build         # Build member image
make up            # Start member service
```

### Available Make Commands

#### Parent Level (`waroenk-parent/Makefile`)

| Command | Description |
|---------|-------------|
| `make help` | Show available commands |
| `make build` | Build all modules |
| `make build-parallel` | Build all modules in parallel |
| `make up` | Start all services |
| `make down` | Stop all services |
| `make logs` | View logs from all services |
| `make infra` | Start PostgreSQL + Redis |
| `make clean` | Remove all images and volumes |

#### Module Level (`member/Makefile`, `catalog/Makefile`, etc.)

| Command | Description |
|---------|-------------|
| `make help` | Show available commands |
| `make build` | Build this module's image |
| `make up` | Start this service |
| `make down` | Stop this service |
| `make logs` | View this service's logs |
| `make infra` | Start PostgreSQL + Redis |
| `make clean` | Remove this module's image |

## Environment Variables

Each module has its own `.env` file. Create them from the examples:

```bash
# For each module
cp member/.env.example member/.env
cp catalog/.env.example catalog/.env
cp cart/.env.example cart/.env
cp api-gateway/.env.example api-gateway/.env
```

### Common Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `HTTP_REST_PORT` | REST API port | varies by module |
| `GRPC_SERVER_PORT` | gRPC server port | varies by module |
| `JAVA_OPTS` | JVM options | `-Xms128m -Xmx256m` |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `docker` |
| `DB_HOSTS` | Database hosts | `postgres:5432` |
| `DB_NAME` | Database name | varies by module |
| `DB_USERNAME` | Database username | `admin` |
| `DB_PASSWORD` | Database password | `admin` |
| `REDIS_MASTER` | Redis Sentinel master name | `mymaster` |
| `REDIS_SENTINEL_NODES` | Redis Sentinel nodes | see .env |

## Docker Networks

The services use the following Docker networks:

| Network | Purpose |
|---------|---------|
| `shared-network` | Inter-service communication |
| `postgres` | PostgreSQL database access |
| `redis-net` | Redis Sentinel access |
| `backend` | Internal backend communication |

## Artifact Naming

All modules produce JARs with the naming pattern: `${project.artifactId}.jar`

- `grpc-contract/target/grpc-contract.jar`
- `member/target/member.jar`
- `catalog/target/catalog.jar`
- `cart/target/cart.jar`
- `api-gateway/target/api-gateway.jar`

## Local Development

For local development without Docker:

```bash
cd waroenk-parent

# Install grpc-contract to local .m2
mvn install -pl grpc-contract

# Build a specific module
mvn package -pl member

# Run a module
java -jar member/target/member.jar
```

## Troubleshooting

### Module can't find grpc-contract

Ensure you build from the parent directory context:

```bash
# Wrong: Building from module directory with . context
cd member && docker build .

# Correct: Building from module directory with parent context
cd member && docker build -f Dockerfile ..

# Or use docker-compose which handles this automatically
cd member && docker-compose build
```

### BuildKit Cache Issues

If you experience cache issues, clear the BuildKit cache:

```bash
docker builder prune
```

### Network Already Exists

If network creation fails:

```bash
docker network rm shared-network
make ensure-network
```














