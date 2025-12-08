package com.gdn.project.waroenk.gateway.config;

import com.gdn.project.waroenk.gateway.service.RouteResolver;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for managing gRPC channels to backend services.
 * Dynamically creates channels for both static (properties-based) and
 * dynamic (database-registered) services.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class GrpcChannelConfig {

    private final GatewayProperties gatewayProperties;
    private final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();

    // Will be injected after RouteResolver is created
    private RouteResolver routeResolver;

    public void setRouteResolver(RouteResolver routeResolver) {
        this.routeResolver = routeResolver;
    }

    @PostConstruct
    public void initStaticChannels() {
        // Initialize channels for statically configured services
        gatewayProperties.getServices().forEach((serviceName, config) -> {
            log.info("Creating static gRPC channel for service: {} -> {}:{}",
                    serviceName, config.getHost(), config.getPort());
            createChannel(serviceName, config.getHost(), config.getPort(), config.isUseTls());
        });
    }

    @PreDestroy
    public void shutdownChannels() {
        log.info("Shutting down {} gRPC channels...", channels.size());
        channels.forEach((name, channel) -> {
            try {
                if (!channel.isShutdown()) {
                    channel.shutdown();
                    // Don't block for too long during shutdown
                    if (!channel.awaitTermination(2, TimeUnit.SECONDS)) {
                        channel.shutdownNow();
                    }
                }
            } catch (InterruptedException e) {
                channel.shutdownNow();
                Thread.currentThread().interrupt();
            }
        });
        channels.clear();
        log.info("All gRPC channels shut down");
    }

    /**
     * Get the channel for a specific service.
     * If no channel exists, tries to create one dynamically.
     */
    public ManagedChannel getChannel(String serviceName) {
        ManagedChannel channel = channels.get(serviceName);

        if (channel == null || channel.isShutdown() || channel.isTerminated()) {
            // Try to get service info from route resolver
            if (routeResolver != null) {
                Optional<RouteResolver.ServiceInfo> serviceInfo = routeResolver.getServiceInfo(serviceName);
                if (serviceInfo.isPresent()) {
                    RouteResolver.ServiceInfo info = serviceInfo.get();
                    channel = createChannel(serviceName, info.host(), info.port(), info.useTls());
                }
            }
        }

        if (channel == null) {
            throw new IllegalArgumentException("No channel configured for service: " + serviceName);
        }

        return channel;
    }

    /**
     * Create or replace a channel for a service.
     * Optimized for stability and resource efficiency:
     * - Disable keepAliveWithoutCalls to release idle connections
     * - Add idle timeout to prevent connection buildup
     * - Limit max retry attempts
     */
    public ManagedChannel createChannel(String serviceName, String host, int port, boolean useTls) {
        // Shutdown existing channel if any (non-blocking)
        ManagedChannel existing = channels.get(serviceName);
        if (existing != null && !existing.isShutdown()) {
            existing.shutdownNow(); // Non-blocking shutdown
        }

        ManagedChannelBuilder<?> builder = ManagedChannelBuilder
                .forAddress(host, port)
                // Keep-alive settings (only when there ARE active calls)
                .keepAliveTime(60, TimeUnit.SECONDS)
                .keepAliveTimeout(20, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(false) // CRITICAL: Don't keep alive without calls!
                // Idle timeout - close connections after 5 min of inactivity
                .idleTimeout(5, TimeUnit.MINUTES)
                // Limit resources
                .maxInboundMessageSize(16 * 1024 * 1024) // 16MB max message
                .maxRetryAttempts(3);

        if (!useTls) {
            builder.usePlaintext();
        }

        ManagedChannel channel = builder.build();
        channels.put(serviceName, channel);

        log.info("Created gRPC channel for service: {} -> {}:{} (TLS: {})",
                serviceName, host, port, useTls);

        return channel;
    }

    /**
     * Remove a channel for a service
     */
    public void removeChannel(String serviceName) {
        ManagedChannel channel = channels.remove(serviceName);
        if (channel != null) {
            try {
                channel.shutdown().awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            log.info("Removed gRPC channel for service: {}", serviceName);
        }
    }

    /**
     * Check if a channel exists for the given service
     */
    public boolean hasChannel(String serviceName) {
        ManagedChannel channel = channels.get(serviceName);
        return channel != null && !channel.isShutdown() && !channel.isTerminated();
    }

    /**
     * Get all configured service names
     */
    public java.util.Set<String> getServiceNames() {
        return channels.keySet();
    }
}
