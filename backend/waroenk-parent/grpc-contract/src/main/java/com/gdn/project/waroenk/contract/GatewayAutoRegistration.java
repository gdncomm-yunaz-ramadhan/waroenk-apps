package com.gdn.project.waroenk.contract;

import io.grpc.BindableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

/**
 * Automatic gateway registration component that discovers and registers
 * all gRPC routes on service startup.
 * 
 * <h2>Features:</h2>
 * <ul>
 *   <li>Auto-discovers routes from @GatewayRoute annotated methods</li>
 *   <li>Always registers on startup (CI/CD friendly)</li>
 *   <li>Uses upsert semantics - creates new routes, updates changed ones</li>
 *   <li>Non-blocking - doesn't delay service startup</li>
 *   <li>Graceful failure handling with automatic retry</li>
 * </ul>
 * 
 * <h2>Spring Integration Example:</h2>
 * <pre>
 * {@code
 * @Configuration
 * public class GatewayConfig {
 *     
 *     @Value("${gateway.host:api-gateway}")
 *     private String gatewayHost;
 *     
 *     @Value("${gateway.grpc.port:6565}")
 *     private int gatewayPort;
 *     
 *     @Value("${grpc.server.port:9090}")
 *     private int grpcPort;
 *     
 *     @Autowired
 *     private Collection<BindableService> grpcServices;
 *     
 *     @PostConstruct
 *     public void registerWithGateway() {
 *         GatewayAutoRegistration.register(
 *             "member",           // service name
 *             gatewayHost,        // gateway host
 *             gatewayPort,        // gateway gRPC port
 *             grpcPort,           // this service's gRPC port
 *             grpcServices        // all gRPC services
 *         );
 *     }
 * }
 * }
 * </pre>
 */
public class GatewayAutoRegistration {

    private static final Logger log = LoggerFactory.getLogger(GatewayAutoRegistration.class);
    private static GatewayRegistrationClient activeClient;

    /**
     * Register all discovered routes with the API Gateway.
     * This method is non-blocking and will not delay service startup.
     * 
     * @param serviceName The name of this service (e.g., "member")
     * @param gatewayHost Gateway hostname
     * @param gatewayPort Gateway gRPC registration port
     * @param serviceGrpcPort This service's gRPC port
     * @param grpcServices Collection of all gRPC BindableService implementations
     */
    public static void register(String serviceName, String gatewayHost, int gatewayPort,
                                int serviceGrpcPort, Collection<? extends BindableService> grpcServices) {
        register(serviceName, gatewayHost, gatewayPort, serviceName, serviceGrpcPort, grpcServices);
    }

    /**
     * Register all discovered routes with the API Gateway.
     * 
     * @param serviceName The name of this service (e.g., "member")
     * @param gatewayHost Gateway hostname
     * @param gatewayPort Gateway gRPC registration port
     * @param serviceHost This service's hostname (for Docker, use service name)
     * @param serviceGrpcPort This service's gRPC port
     * @param grpcServices Collection of all gRPC BindableService implementations
     */
    public static void register(String serviceName, String gatewayHost, int gatewayPort,
                                String serviceHost, int serviceGrpcPort,
                                Collection<? extends BindableService> grpcServices) {
        log.info("🚀 Starting gateway auto-registration for service: {}", serviceName);

        // Discover routes from annotated services
        GatewayAutoDiscoverer discoverer = new GatewayAutoDiscoverer();
        List<GatewayRegistrationClient.Route> routes = discoverer.discoverRoutes(grpcServices);

        if (routes.isEmpty()) {
            log.warn("No routes discovered for service {}. Make sure gRPC methods are annotated with @GatewayRoute", serviceName);
            return;
        }

        log.info("Discovered {} routes for service {}", routes.size(), serviceName);

        // Register asynchronously
        activeClient = new GatewayRegistrationClient(gatewayHost, gatewayPort);
        activeClient.registerAsync(serviceName, serviceHost, serviceGrpcPort, false, routes)
                .thenAccept(success -> {
                    if (success) {
                        log.info("✅ Service {} registered with gateway ({} routes)", serviceName, routes.size());
                    } else {
                        log.info("⏳ Service {} gateway registration pending (will retry)", serviceName);
                    }
                })
                .exceptionally(ex -> {
                    log.warn("⚠️ Gateway registration failed (service continues): {}", ex.getMessage());
                    return null;
                });
    }

    /**
     * Register with manually specified routes (for services not using annotations).
     * 
     * @param serviceName The name of this service
     * @param gatewayHost Gateway hostname
     * @param gatewayPort Gateway gRPC registration port
     * @param serviceGrpcPort This service's gRPC port
     * @param routes List of routes to register
     */
    public static void registerRoutes(String serviceName, String gatewayHost, int gatewayPort,
                                      int serviceGrpcPort, List<GatewayRegistrationClient.Route> routes) {
        registerRoutes(serviceName, gatewayHost, gatewayPort, serviceName, serviceGrpcPort, routes);
    }

    /**
     * Register with manually specified routes.
     */
    public static void registerRoutes(String serviceName, String gatewayHost, int gatewayPort,
                                      String serviceHost, int serviceGrpcPort,
                                      List<GatewayRegistrationClient.Route> routes) {
        log.info("🚀 Starting gateway registration for service: {} ({} routes)", serviceName, routes.size());

        activeClient = new GatewayRegistrationClient(gatewayHost, gatewayPort);
        activeClient.registerAsync(serviceName, serviceHost, serviceGrpcPort, false, routes)
                .thenAccept(success -> {
                    if (success) {
                        log.info("✅ Service {} registered with gateway ({} routes)", serviceName, routes.size());
                    } else {
                        log.info("⏳ Service {} gateway registration pending (will retry)", serviceName);
                    }
                })
                .exceptionally(ex -> {
                    log.warn("⚠️ Gateway registration failed (service continues): {}", ex.getMessage());
                    return null;
                });
    }

    /**
     * Unregister from gateway (call on shutdown)
     */
    public static void unregister() {
        if (activeClient != null) {
            activeClient.unregister();
            activeClient = null;
        }
    }

    /**
     * Shutdown the registration client
     */
    public static void shutdown() {
        if (activeClient != null) {
            activeClient.shutdown();
            activeClient = null;
        }
    }
}


