package com.gdn.project.waroenk.contract;

import com.gdn.project.waroenk.gateway.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Client for microservices to register with the API Gateway.
 * 
 * <h2>Features:</h2>
 * <ul>
 *   <li>Async, non-blocking registration (won't block service startup)</li>
 *   <li>Automatic heartbeat management</li>
 *   <li>Bulk upsert registration (creates or updates all routes)</li>
 *   <li>Graceful failure handling (ignores errors if gateway unreachable)</li>
 *   <li>Automatic retry with exponential backoff</li>
 * </ul>
 * 
 * <h2>Usage with Auto-Discovery:</h2>
 * <pre>
 * {@code
 * @Component
 * @RequiredArgsConstructor
 * public class GatewayRegistration {
 *     
 *     private final Collection<BindableService> grpcServices;
 *     
 *     @PostConstruct
 *     public void register() {
 *         GatewayAutoDiscoverer discoverer = new GatewayAutoDiscoverer();
 *         List<Route> routes = discoverer.discoverRoutes(grpcServices);
 *         
 *         GatewayRegistrationClient.forService("member")
 *             .gateway("api-gateway", 6565)
 *             .serviceAddress("member", 9090)
 *             .routes(routes)
 *             .registerAsync();
 *     }
 * }
 * }
 * </pre>
 */
public class GatewayRegistrationClient {

    private static final Logger log = LoggerFactory.getLogger(GatewayRegistrationClient.class);
    private static final int DEFAULT_HEARTBEAT_INTERVAL = 30; // seconds
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final int INITIAL_RETRY_DELAY_SECONDS = 5;

    private final String gatewayHost;
    private final int gatewayPort;
    private final ExecutorService executor;
    private final ScheduledExecutorService heartbeatExecutor;
    private final AtomicBoolean registered = new AtomicBoolean(false);
    private final AtomicInteger retryCount = new AtomicInteger(0);
    private ManagedChannel channel;
    private GatewayRegistrationServiceGrpc.GatewayRegistrationServiceBlockingStub stub;
    private String serviceName;
    private volatile boolean shutdown = false;

    public GatewayRegistrationClient(String gatewayHost, int gatewayPort) {
        this.gatewayHost = gatewayHost;
        this.gatewayPort = gatewayPort;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "gateway-registration");
            t.setDaemon(true);
            return t;
        });
        this.heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "gateway-heartbeat");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Create a new registration builder for a service.
     */
    public static ServiceBuilder forService(String serviceName) {
        return new ServiceBuilder(serviceName);
    }

    /**
     * Register service asynchronously (non-blocking).
     * Always attempts to register - uses upsert semantics.
     * Safe to call during service startup - failures are logged but don't prevent startup.
     */
    public CompletableFuture<Boolean> registerAsync(String serviceName, String serviceHost, int servicePort,
                                                    boolean useTls, List<Route> routes) {
        this.serviceName = serviceName;

        return CompletableFuture.supplyAsync(() -> {
            try {
                return doRegister(serviceName, serviceHost, servicePort, useTls, routes);
            } catch (Exception e) {
                log.warn("Failed to register with gateway (will retry): {}", e.getMessage());
                scheduleRetry(serviceName, serviceHost, servicePort, useTls, routes);
                return false;
            }
        }, executor);
    }

    /**
     * Register service synchronously (blocking).
     * Always attempts to register - uses upsert semantics.
     */
    public boolean register(String serviceName, String serviceHost, int servicePort,
                            boolean useTls, List<Route> routes) {
        this.serviceName = serviceName;
        return doRegister(serviceName, serviceHost, servicePort, useTls, routes);
    }

    /**
     * Perform the actual registration - always sends all routes (upsert).
     * The gateway handles deduplication and updates.
     */
    private boolean doRegister(String serviceName, String serviceHost, int servicePort,
                               boolean useTls, List<Route> routes) {
        try {
            ensureChannel();

            log.info("Registering {} routes for service {} with gateway {}:{}",
                    routes.size(), serviceName, gatewayHost, gatewayPort);

            // Build service definition with ALL routes - gateway handles upsert
            ServiceDefinition.Builder defBuilder = ServiceDefinition.newBuilder()
                    .setName(serviceName)
                    .setProtocol("grpc")
                    .setHost(serviceHost)
                    .setPort(servicePort)
                    .setUseTls(useTls);

            for (Route route : routes) {
                defBuilder.addRoutes(RouteDefinition.newBuilder()
                        .setHttpMethod(route.httpMethod)
                        .setPath(route.path)
                        .setGrpcService(route.grpcService)
                        .setGrpcMethod(route.grpcMethod)
                        .setRequestType(route.requestType != null ? route.requestType : "")
                        .setResponseType(route.responseType != null ? route.responseType : "")
                        .setPublicEndpoint(route.publicEndpoint)
                        .addAllRequiredRoles(route.requiredRoles != null ? route.requiredRoles : List.of())
                        .setRouteHash(computeRouteHash(route))
                        .build());
            }

            RegistrationAck response = stub.registerService(defBuilder.build());

            if (response.getSuccess()) {
                log.info("✅ Service {} registered successfully: {} routes added/updated, {} unchanged",
                        serviceName, response.getRoutesRegistered(), response.getRoutesSkipped());
                registered.set(true);
                retryCount.set(0);
                startHeartbeat();
                return true;
            } else {
                log.warn("⚠️ Service {} registration failed: {}", serviceName, response.getMessage());
                return false;
            }

        } catch (StatusRuntimeException e) {
            log.warn("Failed to register with gateway ({}:{}): {}",
                    gatewayHost, gatewayPort, e.getStatus().getDescription());
            return false;
        } catch (Exception e) {
            log.warn("Unexpected error during registration: {}", e.getMessage());
            return false;
        }
    }

    private void ensureChannel() {
        if (channel == null || channel.isShutdown()) {
            channel = ManagedChannelBuilder.forAddress(gatewayHost, gatewayPort)
                    .usePlaintext()
                    .keepAliveTime(30, TimeUnit.SECONDS)
                    .keepAliveTimeout(10, TimeUnit.SECONDS)
                    .build();
            stub = GatewayRegistrationServiceGrpc.newBlockingStub(channel);
        }
    }

    private void scheduleRetry(String serviceName, String serviceHost, int servicePort,
                               boolean useTls, List<Route> routes) {
        if (shutdown) return;
        
        int currentRetry = retryCount.incrementAndGet();
        if (currentRetry > MAX_RETRY_ATTEMPTS) {
            log.warn("Max retry attempts ({}) reached for gateway registration. Service will continue without gateway.",
                    MAX_RETRY_ATTEMPTS);
            return;
        }

        // Exponential backoff: 5s, 10s, 20s, 40s, 80s
        int delaySeconds = INITIAL_RETRY_DELAY_SECONDS * (1 << (currentRetry - 1));
        
        log.info("Scheduling gateway registration retry #{} in {}s", currentRetry, delaySeconds);
        
        heartbeatExecutor.schedule(() -> {
            if (!shutdown) {
                try {
                    boolean success = doRegister(serviceName, serviceHost, servicePort, useTls, routes);
                    if (!success && !shutdown) {
                        scheduleRetry(serviceName, serviceHost, servicePort, useTls, routes);
                    }
                } catch (Exception e) {
                    log.warn("Retry registration failed: {}", e.getMessage());
                    if (!shutdown) {
                        scheduleRetry(serviceName, serviceHost, servicePort, useTls, routes);
                    }
                }
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    private void startHeartbeat() {
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            try {
                if (serviceName != null && channel != null && !channel.isShutdown() && !shutdown) {
                    HeartbeatAck response = stub.heartbeat(
                            ServicePing.newBuilder()
                                    .setServiceName(serviceName)
                                    .setStatus(ServiceStatus.HEALTHY)
                                    .build()
                    );
                    if (!response.getActive()) {
                        log.warn("Gateway reports service {} as inactive", serviceName);
                    }
                }
            } catch (Exception e) {
                log.debug("Heartbeat failed: {}", e.getMessage());
            }
        }, DEFAULT_HEARTBEAT_INTERVAL, DEFAULT_HEARTBEAT_INTERVAL, TimeUnit.SECONDS);
    }

    /**
     * Unregister from gateway
     */
    public void unregister() {
        if (serviceName != null && channel != null && !channel.isShutdown()) {
            try {
                stub.unregisterService(UnregisterRequest.newBuilder()
                        .setServiceName(serviceName)
                        .build());
                log.info("Service {} unregistered from gateway", serviceName);
            } catch (Exception e) {
                log.debug("Failed to unregister: {}", e.getMessage());
            }
        }
        shutdown();
    }

    /**
     * Shutdown the client
     */
    public void shutdown() {
        shutdown = true;
        heartbeatExecutor.shutdown();
        executor.shutdown();
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }

    private String computeRouteHash(Route route) {
        try {
            String content = route.httpMethod + ":" + route.path + ":" +
                    route.grpcService + ":" + route.grpcMethod + ":" +
                    (route.requestType != null ? route.requestType : "") + ":" +
                    (route.responseType != null ? route.responseType : "") + ":" +
                    route.publicEndpoint + ":" +
                    (route.requiredRoles != null ? String.join(",", route.requiredRoles) : "");

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Route definition for registration.
     * 
     * With gRPC Server Reflection enabled on target services, the gateway can
     * dynamically discover request/response types at runtime. Therefore:
     * - requestType and responseType are now OPTIONAL (can be null or empty)
     * - Only httpMethod, path, grpcService, and grpcMethod are required
     */
    public record Route(
            String httpMethod,
            String path,
            String grpcService,
            String grpcMethod,
            String requestType,
            String responseType,
            boolean publicEndpoint,
            List<String> requiredRoles
    ) {
        /**
         * Full constructor with request/response types (backward compatible)
         */
        public Route(String httpMethod, String path, String grpcService, String grpcMethod,
                     String requestType, String responseType, boolean publicEndpoint) {
            this(httpMethod, path, grpcService, grpcMethod, requestType, responseType, publicEndpoint, List.of());
        }

        /**
         * Simplified constructor WITHOUT request/response types.
         * Uses gRPC Server Reflection to discover types at runtime.
         * This is the recommended approach for new routes.
         */
        public Route(String httpMethod, String path, String grpcService, String grpcMethod,
                     boolean publicEndpoint, List<String> requiredRoles) {
            this(httpMethod, path, grpcService, grpcMethod, null, null, publicEndpoint, requiredRoles);
        }

        /**
         * Minimal constructor for public endpoints without roles.
         */
        public Route(String httpMethod, String path, String grpcService, String grpcMethod, boolean publicEndpoint) {
            this(httpMethod, path, grpcService, grpcMethod, null, null, publicEndpoint, List.of());
        }
    }

    /**
     * Fluent builder for service registration
     */
    public static class ServiceBuilder {
        private final String serviceName;
        private String gatewayHost = "api-gateway";
        private int gatewayPort = 6565;
        private String serviceHost;
        private int servicePort = 9090;
        private boolean useTls = false;
        private final List<Route> routes = new ArrayList<>();

        public ServiceBuilder(String serviceName) {
            this.serviceName = serviceName;
            // Default service host is the service name (for Docker)
            this.serviceHost = serviceName;
        }

        public ServiceBuilder gateway(String host, int port) {
            this.gatewayHost = host;
            this.gatewayPort = port;
            return this;
        }

        public ServiceBuilder serviceAddress(String host, int port) {
            this.serviceHost = host;
            this.servicePort = port;
            return this;
        }

        public ServiceBuilder useTls(boolean useTls) {
            this.useTls = useTls;
            return this;
        }

        public ServiceBuilder routes(List<Route> routes) {
            this.routes.addAll(routes);
            return this;
        }

        /**
         * Add a route with explicit request/response types (backward compatible)
         */
        public ServiceBuilder addRoute(String httpMethod, String path, String grpcService, String grpcMethod,
                                        String requestType, String responseType, boolean publicEndpoint) {
            routes.add(new Route(httpMethod, path, grpcService, grpcMethod,
                    requestType, responseType, publicEndpoint, List.of()));
            return this;
        }

        /**
         * Add a route WITHOUT request/response types (uses reflection).
         * This is the recommended approach for new routes.
         */
        public ServiceBuilder addRoute(String httpMethod, String path, String grpcService, String grpcMethod,
                                        boolean publicEndpoint) {
            routes.add(new Route(httpMethod, path, grpcService, grpcMethod, publicEndpoint));
            return this;
        }

        public ServiceBuilder addRoute(Route route) {
            routes.add(route);
            return this;
        }

        /**
         * Register asynchronously (non-blocking, recommended)
         */
        public CompletableFuture<Boolean> registerAsync() {
            GatewayRegistrationClient client = new GatewayRegistrationClient(gatewayHost, gatewayPort);
            return client.registerAsync(serviceName, serviceHost, servicePort, useTls, routes);
        }

        /**
         * Register synchronously (blocking)
         */
        public boolean register() {
            GatewayRegistrationClient client = new GatewayRegistrationClient(gatewayHost, gatewayPort);
            return client.register(serviceName, serviceHost, servicePort, useTls, routes);
        }

        /**
         * Get the client instance for manual control
         */
        public GatewayRegistrationClient build() {
            return new GatewayRegistrationClient(gatewayHost, gatewayPort);
        }
    }

    // Legacy Builder for backwards compatibility
    @Deprecated
    public static class Builder extends ServiceBuilder {
        public Builder(String gatewayHost, int gatewayPort) {
            super("unknown");
            gateway(gatewayHost, gatewayPort);
        }

        public Builder service(String name, String host, int port) {
            return (Builder) new ServiceBuilder(name)
                    .gateway(((ServiceBuilder) this).gatewayHost, ((ServiceBuilder) this).gatewayPort)
                    .serviceAddress(host, port);
        }
    }
}
