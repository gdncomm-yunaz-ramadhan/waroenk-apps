package com.gdn.project.waroenk.gateway.controller;

import com.gdn.project.waroenk.gateway.config.GatewayProperties;
import com.gdn.project.waroenk.gateway.service.DynamicRoutingRegistry;
import com.gdn.project.waroenk.gateway.service.DynamicRoutingRegistry.CachedRoute;
import com.gdn.project.waroenk.gateway.service.ReflectionGrpcClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Dynamic OpenAPI documentation generator.
 * Generates OpenAPI 3.0 spec from registered routes (static + dynamic).
 * 
 * Access Swagger UI at: /swagger-ui.html
 * Access OpenAPI JSON at: /api-docs/dynamic
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "API Documentation", description = "Dynamic OpenAPI documentation for all registered routes")
public class DynamicOpenApiController {

    private final GatewayProperties gatewayProperties;
    private final DynamicRoutingRegistry dynamicRegistry;

    @Value("${info.app.version:1.0.0}")
    private String appVersion;

    @Value("${spring.application.name:api-gateway}")
    private String appName;

    /**
     * Generate OpenAPI 3.0 spec dynamically from all registered routes.
     * This includes both static (from properties) and dynamic (from database) routes.
     */
    @GetMapping(value = "/api-docs/dynamic", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Dynamic OpenAPI Spec", description = "Returns OpenAPI 3.0 specification generated from all registered routes")
    public ResponseEntity<Map<String, Object>> getDynamicOpenApiSpec() {
        Map<String, Object> openApi = new LinkedHashMap<>();

        // OpenAPI version
        openApi.put("openapi", "3.0.3");

        // Info
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("title", appName + " - Dynamic API Documentation");
        info.put("description", "Auto-generated API documentation from registered gRPC routes. " +
                "All endpoints proxy to backend gRPC services.");
        info.put("version", appVersion);
        openApi.put("info", info);

        // Servers
        List<Map<String, String>> servers = new ArrayList<>();
        servers.add(Map.of("url", "/", "description", "Current server"));
        openApi.put("servers", servers);

        // Security schemes
        Map<String, Object> components = new LinkedHashMap<>();
        Map<String, Object> securitySchemes = new LinkedHashMap<>();
        securitySchemes.put("bearerAuth", Map.of(
                "type", "http",
                "scheme", "bearer",
                "bearerFormat", "JWT",
                "description", "JWT Authentication token"
        ));
        components.put("securitySchemes", securitySchemes);
        openApi.put("components", components);

        // Paths - collect all routes
        Map<String, Object> paths = new LinkedHashMap<>();
        
        // Add static routes
        for (GatewayProperties.RouteConfig route : gatewayProperties.getRoutes()) {
            for (GatewayProperties.MethodMapping method : route.getMethods()) {
                addPathToSpec(paths, 
                        method.getHttpPath(),
                        method.getHttpMethod().toLowerCase(),
                        route.getService(),
                        route.getGrpcService(),
                        method.getGrpcMethod(),
                        method.isPublicEndpoint() || route.isPublicRoute(),
                        "static");
            }
        }

        // Add dynamic routes
        for (CachedRoute route : dynamicRegistry.getAllRoutes()) {
            addPathToSpec(paths,
                    route.path(),
                    route.httpMethod().toLowerCase(),
                    route.serviceName(),
                    route.grpcServiceName(),
                    route.grpcMethodName(),
                    route.publicEndpoint(),
                    "dynamic");
        }

        openApi.put("paths", paths);

        // Tags - group by service
        Set<String> services = new TreeSet<>();
        gatewayProperties.getRoutes().forEach(r -> services.add(r.getService()));
        dynamicRegistry.getAllRoutes().forEach(r -> services.add(r.serviceName()));

        List<Map<String, String>> tags = services.stream()
                .map(s -> Map.of("name", s, "description", s + " service endpoints"))
                .collect(Collectors.toList());
        openApi.put("tags", tags);

        return ResponseEntity.ok(openApi);
    }

    /**
     * Get a simplified route documentation in a more readable format.
     */
    @GetMapping(value = "/api-docs/routes", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Route Documentation", description = "Returns simplified route documentation grouped by service")
    public ResponseEntity<Map<String, Object>> getRouteDocumentation() {
        Map<String, Object> result = new LinkedHashMap<>();

        // Group routes by service
        Map<String, List<Map<String, Object>>> routesByService = new LinkedHashMap<>();

        // Add static routes
        for (GatewayProperties.RouteConfig route : gatewayProperties.getRoutes()) {
            String service = route.getService();
            routesByService.computeIfAbsent(service, k -> new ArrayList<>());

            for (GatewayProperties.MethodMapping method : route.getMethods()) {
                Map<String, Object> routeInfo = new LinkedHashMap<>();
                routeInfo.put("method", method.getHttpMethod());
                routeInfo.put("path", method.getHttpPath());
                routeInfo.put("grpc_method", route.getGrpcService() + "/" + method.getGrpcMethod());
                routeInfo.put("public", method.isPublicEndpoint() || route.isPublicRoute());
                routeInfo.put("auth_required", !(method.isPublicEndpoint() || route.isPublicRoute()));
                routesByService.get(service).add(routeInfo);
            }
        }

        // Add dynamic routes
        for (CachedRoute route : dynamicRegistry.getAllRoutes()) {
            String service = route.serviceName();
            routesByService.computeIfAbsent(service, k -> new ArrayList<>());

            Map<String, Object> routeInfo = new LinkedHashMap<>();
            routeInfo.put("method", route.httpMethod());
            routeInfo.put("path", route.path());
            routeInfo.put("grpc_method", route.grpcServiceName() + "/" + route.grpcMethodName());
            routeInfo.put("public", route.publicEndpoint());
            routeInfo.put("auth_required", !route.publicEndpoint());
            routesByService.get(service).add(routeInfo);
        }

        result.put("services", routesByService);
        result.put("total_services", routesByService.size());
        result.put("total_routes", routesByService.values().stream().mapToInt(List::size).sum());

        return ResponseEntity.ok(result);
    }

    private void addPathToSpec(Map<String, Object> paths, String path, String method,
                                String service, String grpcService, String grpcMethod,
                                boolean isPublic, String source) {
        // Ensure path entry exists
        @SuppressWarnings("unchecked")
        Map<String, Object> pathItem = (Map<String, Object>) paths.computeIfAbsent(path, k -> new LinkedHashMap<>());

        // Build operation
        Map<String, Object> operation = new LinkedHashMap<>();
        operation.put("tags", List.of(service));
        operation.put("summary", grpcMethod);
        operation.put("description", String.format(
                "Proxies to gRPC: `%s/%s`\n\n" +
                "**Source:** %s\n\n" +
                "**Authentication:** %s",
                grpcService, grpcMethod, source,
                isPublic ? "Not required (public endpoint)" : "Required (JWT Bearer token)"
        ));
        operation.put("operationId", grpcMethod + "_" + method.toUpperCase());

        // Security
        if (!isPublic) {
            operation.put("security", List.of(Map.of("bearerAuth", List.of())));
        }

        // Parameters for GET/DELETE with path variables
        if (path.contains("{")) {
            List<Map<String, Object>> parameters = new ArrayList<>();
            // Extract path variables like {id}, {userId}, etc.
            String[] parts = path.split("/");
            for (String part : parts) {
                if (part.startsWith("{") && part.endsWith("}")) {
                    String paramName = part.substring(1, part.length() - 1);
                    parameters.add(Map.of(
                            "name", paramName,
                            "in", "path",
                            "required", true,
                            "schema", Map.of("type", "string"),
                            "description", paramName + " parameter"
                    ));
                }
            }
            if (!parameters.isEmpty()) {
                operation.put("parameters", parameters);
            }
        }

        // Request body for POST/PUT/PATCH
        if (method.equals("post") || method.equals("put") || method.equals("patch")) {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("required", true);
            requestBody.put("content", Map.of(
                    "application/json", Map.of(
                            "schema", Map.of(
                                    "type", "object",
                                    "description", "Request body (JSON). Structure matches the gRPC request message."
                            )
                    )
            ));
            operation.put("requestBody", requestBody);
        }

        // Responses
        Map<String, Object> responses = new LinkedHashMap<>();
        responses.put("200", Map.of(
                "description", "Successful response",
                "content", Map.of(
                        "application/json", Map.of(
                                "schema", Map.of(
                                        "type", "object",
                                        "description", "Response from gRPC service (JSON-serialized protobuf)"
                                )
                        )
                )
        ));
        responses.put("400", Map.of("description", "Bad Request - Invalid input"));
        responses.put("401", Map.of("description", "Unauthorized - Invalid or missing JWT token"));
        responses.put("403", Map.of("description", "Forbidden - Insufficient permissions"));
        responses.put("404", Map.of("description", "Not Found - Resource or route not found"));
        responses.put("500", Map.of("description", "Internal Server Error"));
        responses.put("503", Map.of("description", "Service Unavailable - Backend gRPC service is down"));
        operation.put("responses", responses);

        pathItem.put(method, operation);
    }
}





