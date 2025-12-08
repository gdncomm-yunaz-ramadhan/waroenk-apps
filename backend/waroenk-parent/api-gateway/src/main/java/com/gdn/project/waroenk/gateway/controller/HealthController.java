package com.gdn.project.waroenk.gateway.controller;

import com.gdn.project.waroenk.gateway.config.GatewayProperties;
import com.gdn.project.waroenk.gateway.config.GrpcChannelConfig;
import com.gdn.project.waroenk.gateway.service.DynamicRoutingRegistry;
import com.gdn.project.waroenk.gateway.service.DynamicRoutingRegistry.CachedRoute;
import com.gdn.project.waroenk.gateway.service.DynamicRoutingRegistry.CachedService;
import com.gdn.project.waroenk.gateway.service.RouteResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Health check, status, and service information controller
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Health & Info", description = "Health check and gateway information endpoints")
public class HealthController {

    private final GatewayProperties gatewayProperties;
    private final GrpcChannelConfig channelConfig;
    private final DynamicRoutingRegistry dynamicRegistry;
    private final RouteResolver routeResolver;

    @Value("${info.app.version:1.0.0}")
    private String appVersion;

    @Value("${spring.application.name:api-gateway}")
    private String appName;

    @Value("${grpc.server.port:6565}")
    private int grpcServerPort;

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Returns the health status of the gateway")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/info")
    @Operation(summary = "Application info", description = "Returns information about the gateway")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("name", appName);
        response.put("version", appVersion);
        response.put("grpc_registration_port", grpcServerPort);
        response.put("timestamp", LocalDateTime.now());

        // Count routes
        int staticRoutes = gatewayProperties.getRoutes().stream()
                .mapToInt(r -> r.getMethods().size())
                .sum();
        int dynamicRoutes = dynamicRegistry.getAllRoutes().size();

        Map<String, Object> routeStats = new LinkedHashMap<>();
        routeStats.put("static", staticRoutes);
        routeStats.put("dynamic", dynamicRoutes);
        routeStats.put("total", staticRoutes + dynamicRoutes);
        response.put("routes", routeStats);

        // Count services
        Map<String, Object> serviceStats = new LinkedHashMap<>();
        serviceStats.put("static", gatewayProperties.getServices().size());
        serviceStats.put("dynamic", dynamicRegistry.getAllServices().size());
        response.put("services", serviceStats);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/services")
    @Operation(summary = "List services", description = "Returns all registered services (static + dynamic)")
    public ResponseEntity<Map<String, Object>> services() {
        Map<String, Object> response = new LinkedHashMap<>();

        // Static services from properties
        List<Map<String, Object>> staticServices = new ArrayList<>();
        gatewayProperties.getServices().forEach((name, config) -> {
            Map<String, Object> serviceInfo = new LinkedHashMap<>();
            serviceInfo.put("name", name);
            serviceInfo.put("host", config.getHost());
            serviceInfo.put("port", config.getPort());
            serviceInfo.put("use_tls", config.isUseTls());
            serviceInfo.put("source", "static");
            serviceInfo.put("connected", channelConfig.hasChannel(name));
            staticServices.add(serviceInfo);
        });
        response.put("static_services", staticServices);

        // Dynamic services from database
        List<Map<String, Object>> dynamicServices = new ArrayList<>();
        for (CachedService service : dynamicRegistry.getAllServices()) {
            Map<String, Object> serviceInfo = new LinkedHashMap<>();
            serviceInfo.put("name", service.name());
            serviceInfo.put("host", service.host());
            serviceInfo.put("port", service.port());
            serviceInfo.put("use_tls", service.useTls());
            serviceInfo.put("source", "dynamic");
            serviceInfo.put("last_heartbeat", service.lastHeartbeat());
            serviceInfo.put("connected", channelConfig.hasChannel(service.name()));
            dynamicServices.add(serviceInfo);
        }
        response.put("dynamic_services", dynamicServices);

        response.put("total_services", staticServices.size() + dynamicServices.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/routes")
    @Operation(summary = "List routes", description = "Returns all configured routes (static + dynamic)")
    public ResponseEntity<Map<String, Object>> routes() {
        Map<String, Object> response = new LinkedHashMap<>();

        // Static routes from properties
        List<Map<String, Object>> staticRoutes = new ArrayList<>();
        for (GatewayProperties.RouteConfig route : gatewayProperties.getRoutes()) {
            for (GatewayProperties.MethodMapping method : route.getMethods()) {
                Map<String, Object> routeInfo = new LinkedHashMap<>();
                routeInfo.put("http_method", method.getHttpMethod());
                routeInfo.put("path", method.getHttpPath());
                routeInfo.put("service", route.getService());
                routeInfo.put("grpc_service", route.getGrpcService());
                routeInfo.put("grpc_method", method.getGrpcMethod());
                routeInfo.put("public", method.isPublicEndpoint() || route.isPublicRoute());
                routeInfo.put("source", "static");
                staticRoutes.add(routeInfo);
            }
        }
        response.put("static_routes", staticRoutes);

        // Dynamic routes from database
        List<Map<String, Object>> dynamicRoutes = new ArrayList<>();
        for (CachedRoute route : dynamicRegistry.getAllRoutes()) {
            Map<String, Object> routeInfo = new LinkedHashMap<>();
            routeInfo.put("http_method", route.httpMethod());
            routeInfo.put("path", route.path());
            routeInfo.put("service", route.serviceName());
            routeInfo.put("grpc_service", route.grpcServiceName());
            routeInfo.put("grpc_method", route.grpcMethodName());
            routeInfo.put("public", route.publicEndpoint());
            routeInfo.put("source", "dynamic");
            dynamicRoutes.add(routeInfo);
        }
        response.put("dynamic_routes", dynamicRoutes);

        response.put("total_routes", staticRoutes.size() + dynamicRoutes.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/routes/summary")
    @Operation(summary = "Routes summary", description = "Returns route count grouped by service")
    public ResponseEntity<Map<String, Object>> routesSummary() {
        Map<String, Object> response = new LinkedHashMap<>();

        // Group static routes by service
        Map<String, Long> staticByService = gatewayProperties.getRoutes().stream()
                .collect(Collectors.groupingBy(
                        GatewayProperties.RouteConfig::getService,
                        Collectors.summingLong(r -> r.getMethods().size())
                ));

        // Group dynamic routes by service
        Map<String, Long> dynamicByService = dynamicRegistry.getAllRoutes().stream()
                .collect(Collectors.groupingBy(
                        CachedRoute::serviceName,
                        Collectors.counting()
                ));

        // Merge and build summary
        Set<String> allServices = new HashSet<>();
        allServices.addAll(staticByService.keySet());
        allServices.addAll(dynamicByService.keySet());

        List<Map<String, Object>> summary = new ArrayList<>();
        for (String service : allServices) {
            Map<String, Object> serviceRoutes = new LinkedHashMap<>();
            serviceRoutes.put("service", service);
            serviceRoutes.put("static_routes", staticByService.getOrDefault(service, 0L));
            serviceRoutes.put("dynamic_routes", dynamicByService.getOrDefault(service, 0L));
            serviceRoutes.put("total", staticByService.getOrDefault(service, 0L) +
                    dynamicByService.getOrDefault(service, 0L));
            summary.add(serviceRoutes);
        }

        response.put("by_service", summary);
        response.put("total_services", allServices.size());

        return ResponseEntity.ok(response);
    }
}
