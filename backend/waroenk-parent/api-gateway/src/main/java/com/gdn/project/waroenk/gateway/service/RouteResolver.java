package com.gdn.project.waroenk.gateway.service;

import com.gdn.project.waroenk.gateway.config.GatewayProperties;
import com.gdn.project.waroenk.gateway.config.GatewayProperties.MethodMapping;
import com.gdn.project.waroenk.gateway.config.GatewayProperties.RouteConfig;
import com.gdn.project.waroenk.gateway.service.DynamicRoutingRegistry.CachedRoute;
import com.gdn.project.waroenk.gateway.service.DynamicRoutingRegistry.CachedService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.*;

/**
 * Service to resolve HTTP requests to gRPC service/method configurations.
 * Now combines static (properties-based) and dynamic (database-based) routes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteResolver {

    private final GatewayProperties gatewayProperties;
    private final DynamicRoutingRegistry dynamicRegistry;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // Cache for static routes (from properties): "METHOD:PATH" -> ResolvedRoute
    private final Map<String, ResolvedRoute> staticRouteCache = new HashMap<>();

    @PostConstruct
    public void buildStaticRouteCache() {
        log.info("Building static route cache from properties...");

        for (RouteConfig route : gatewayProperties.getRoutes()) {
            for (MethodMapping method : route.getMethods()) {
                String key = buildKey(method.getHttpMethod(), method.getHttpPath());

                ResolvedRoute resolvedRoute = new ResolvedRoute(
                        route.getService(),
                        route.getGrpcService(),
                        method.getGrpcMethod(),
                        method.getRequestType(),
                        method.getResponseType(),
                        method.isPublicEndpoint() || route.isPublicRoute(),
                        method.getRequiredRoles(),
                        method.getHttpPath()  // Include the HTTP path pattern
                );

                staticRouteCache.put(key, resolvedRoute);
                log.debug("Static route: {} -> {}.{}", key, route.getGrpcService(), method.getGrpcMethod());
            }
        }

        log.info("Static route cache built with {} routes", staticRouteCache.size());
    }

    /**
     * Resolve the route for a given HTTP method and path.
     * First checks dynamic routes (database), then falls back to static routes (properties).
     */
    public Optional<ResolvedRoute> resolve(String httpMethod, String path) {
        String key = buildKey(httpMethod, path);

        // 1. Try dynamic routes first (registered by microservices)
        Optional<CachedRoute> dynamicRoute = dynamicRegistry.resolveRoute(httpMethod, path);
        if (dynamicRoute.isPresent()) {
            return Optional.of(fromCachedRoute(dynamicRoute.get()));
        }

        // 2. Try exact match in static routes
        ResolvedRoute staticRoute = staticRouteCache.get(key);
        if (staticRoute != null) {
            return Optional.of(staticRoute);
        }

        // 3. Try pattern matching in static routes
        for (Map.Entry<String, ResolvedRoute> entry : staticRouteCache.entrySet()) {
            String[] parts = entry.getKey().split(":", 2);
            if (parts.length == 2 && parts[0].equalsIgnoreCase(httpMethod)) {
                if (pathMatcher.match(parts[1], path)) {
                    return Optional.of(entry.getValue());
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Get all routes (both static and dynamic)
     */
    public List<ResolvedRoute> getAllRoutes() {
        List<ResolvedRoute> allRoutes = new ArrayList<>();

        // Add static routes
        allRoutes.addAll(staticRouteCache.values());

        // Add dynamic routes
        for (CachedRoute cachedRoute : dynamicRegistry.getAllRoutes()) {
            allRoutes.add(fromCachedRoute(cachedRoute));
        }

        return allRoutes;
    }

    /**
     * Get the service config for a service name.
     * First checks dynamic registry, then falls back to static config.
     */
    public Optional<ServiceInfo> getServiceInfo(String serviceName) {
        // Try dynamic registry first
        Optional<CachedService> dynamicService = dynamicRegistry.getService(serviceName);
        if (dynamicService.isPresent()) {
            CachedService s = dynamicService.get();
            return Optional.of(new ServiceInfo(s.name(), s.host(), s.port(), s.useTls()));
        }

        // Fall back to static config
        GatewayProperties.ServiceConfig staticConfig = gatewayProperties.getServices().get(serviceName);
        if (staticConfig != null) {
            return Optional.of(new ServiceInfo(
                    serviceName,
                    staticConfig.getHost(),
                    staticConfig.getPort(),
                    staticConfig.isUseTls()
            ));
        }

        return Optional.empty();
    }

    /**
     * Extract path variables from a path based on the pattern
     */
    public Map<String, String> extractPathVariables(String pattern, String path) {
        return pathMatcher.extractUriTemplateVariables(pattern, path);
    }

    private String buildKey(String method, String path) {
        return method.toUpperCase() + ":" + path;
    }

    private ResolvedRoute fromCachedRoute(CachedRoute cached) {
        return new ResolvedRoute(
                cached.serviceName(),
                cached.grpcServiceName(),
                cached.grpcMethodName(),
                cached.requestType(),
                cached.responseType(),
                cached.publicEndpoint(),
                cached.requiredRoles(),
                cached.path()  // Include the HTTP path pattern
        );
    }

    /**
     * Resolved route information containing all details needed to make a gRPC call
     */
    public record ResolvedRoute(
            String serviceName,
            String grpcServiceName,
            String grpcMethodName,
            String requestType,
            String responseType,
            boolean publicEndpoint,
            List<String> requiredRoles,
            String httpPathPattern  // The pattern used to match, e.g. /api/merchant/{id}
    ) {
    }

    /**
     * Service connection information
     */
    public record ServiceInfo(
            String name,
            String host,
            int port,
            boolean useTls
    ) {
    }
}
