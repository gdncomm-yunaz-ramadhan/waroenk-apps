package com.gdn.project.waroenk.gateway.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "route_registry")
public class RouteRegistryEntity {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceRegistryEntity service;

    @Column(name = "http_method", nullable = false, length = 20)
    private String httpMethod;

    @Column(nullable = false, length = 500)
    private String path;

    @Column(name = "grpc_service", nullable = false, length = 500)
    private String grpcService;

    @Column(name = "grpc_method", nullable = false, length = 500)
    private String grpcMethod;

    @Column(name = "request_type", length = 500)
    private String requestType;

    @Column(name = "response_type", length = 500)
    private String responseType;

    @Column(name = "public_endpoint", nullable = false)
    @Builder.Default
    private boolean publicEndpoint = false;

    @Column(name = "required_roles", columnDefinition = "TEXT[]")
    private String[] requiredRoles;

    @Column(name = "route_hash", length = 64)
    private String routeHash;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    /**
     * Get required roles as a list
     */
    public List<String> getRequiredRolesList() {
        if (requiredRoles == null) {
            return List.of();
        }
        return List.of(requiredRoles);
    }

    /**
     * Set required roles from a list
     */
    public void setRequiredRolesList(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            this.requiredRoles = null;
        } else {
            this.requiredRoles = roles.toArray(new String[0]);
        }
    }
}











