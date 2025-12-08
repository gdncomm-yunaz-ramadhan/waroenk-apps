-- ===========================================
-- API Gateway Schema
-- Stores service registrations and route mappings
-- ===========================================

-- Service Registry: stores registered microservices
CREATE TABLE service_registry (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE,
    protocol VARCHAR(50) NOT NULL DEFAULT 'grpc',
    host VARCHAR(200) NOT NULL,
    port INT NOT NULL,
    use_tls BOOLEAN NOT NULL DEFAULT FALSE,
    descriptor_url TEXT,
    version VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_heartbeat TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for service_registry
CREATE INDEX idx_service_registry_name ON service_registry(name);
CREATE INDEX idx_service_registry_host_port ON service_registry(host, port);
CREATE INDEX idx_service_registry_active ON service_registry(active);

-- Route Registry: stores HTTP to gRPC route mappings
CREATE TABLE route_registry (
    id UUID PRIMARY KEY,
    service_id UUID NOT NULL REFERENCES service_registry(id) ON DELETE CASCADE,
    http_method VARCHAR(20) NOT NULL,
    path VARCHAR(500) NOT NULL,
    grpc_service VARCHAR(500) NOT NULL,
    grpc_method VARCHAR(500) NOT NULL,
    request_type VARCHAR(500),
    response_type VARCHAR(500),
    public_endpoint BOOLEAN NOT NULL DEFAULT FALSE,
    required_roles TEXT[], -- Array of required roles
    route_hash VARCHAR(64), -- SHA-256 hash for change detection
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Unique constraint on HTTP method + path (only one handler per route)
CREATE UNIQUE INDEX idx_route_unique ON route_registry(http_method, path);
CREATE INDEX idx_route_service ON route_registry(service_id);
CREATE INDEX idx_route_hash ON route_registry(route_hash);
CREATE INDEX idx_route_grpc_service ON route_registry(grpc_service);

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers for auto-updating updated_at
CREATE TRIGGER update_service_registry_updated_at
    BEFORE UPDATE ON service_registry
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_route_registry_updated_at
    BEFORE UPDATE ON route_registry
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();


