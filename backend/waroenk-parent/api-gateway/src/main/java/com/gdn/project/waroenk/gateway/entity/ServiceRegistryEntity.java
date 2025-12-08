package com.gdn.project.waroenk.gateway.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "service_registry")
public class ServiceRegistryEntity {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(nullable = false, unique = true, length = 200)
    private String name;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String protocol = "grpc";

    @Column(nullable = false, length = 200)
    private String host;

    @Column(nullable = false)
    private int port;

    @Column(name = "use_tls", nullable = false)
    @Builder.Default
    private boolean useTls = false;

    @Column(name = "descriptor_url")
    private String descriptorUrl;

    @Column(length = 100)
    private String version;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RouteRegistryEntity> routes = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public void addRoute(RouteRegistryEntity route) {
        routes.add(route);
        route.setService(this);
    }

    public void removeRoute(RouteRegistryEntity route) {
        routes.remove(route);
        route.setService(null);
    }
}


