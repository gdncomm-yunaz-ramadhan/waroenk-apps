package com.gdn.project.waroenk.gateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdn.project.waroenk.gateway.entity.RouteRegistryEntity;
import com.gdn.project.waroenk.gateway.entity.ServiceRegistryEntity;
import com.gdn.project.waroenk.gateway.repository.RouteRegistryRepository;
import com.gdn.project.waroenk.gateway.repository.ServiceRegistryRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.util.AntPathMatcher;

/**
 * Dynamic routing registry that combines PostgreSQL persistence with Redis caching.
 * Routes are loaded from DB on startup and cached in Redis + memory for fast lookups.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicRoutingRegistry {

    private static final String REDIS_ROUTE_KEY_PREFIX = "gateway:route:";
    private static final String REDIS_SERVICE_KEY_PREFIX = "gateway:service:";
    private static final String REDIS_ROUTES_SET = "gateway:routes:all";
    private static final Duration REDIS_TTL = Duration.ofHours(24);
    private static final Duration HEARTBEAT_TIMEOUT = Duration.ofMinutes(2);

    private final ServiceRegistryRepository serviceRepository;
    private final RouteRegistryRepository routeRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    // In-memory cache for ultra-fast lookups
    private final Map<String, CachedRoute> routeCache = new ConcurrentHashMap<>();
    private final Map<String, CachedService> serviceCache = new ConcurrentHashMap<>();
    
    // Path matcher for handling path variables like {id}
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @PostConstruct
    public void initialize() {
        log.info("Initializing dynamic routing registry...");
        loadRoutesFromDatabase();
        log.info("Routing registry initialized with {} routes from {} services",
                routeCache.size(), serviceCache.size());
    }

    /**
     * Load all active routes from database into cache
     */
    @Transactional(readOnly = true)
    public void loadRoutesFromDatabase() {
        // Clear existing caches
        routeCache.clear();
        serviceCache.clear();

        // Load active services with routes eagerly fetched to avoid lazy loading issues
        List<ServiceRegistryEntity> services = serviceRepository.findByActiveTrueWithRoutes();
        for (ServiceRegistryEntity service : services) {
            CachedService cachedService = toCachedService(service);
            serviceCache.put(service.getName(), cachedService);

            // Cache each route (routes are already fetched)
            for (RouteRegistryEntity route : service.getRoutes()) {
                CachedRoute cachedRoute = toCachedRoute(route, cachedService);
                String routeKey = buildRouteKey(route.getHttpMethod(), route.getPath());
                routeCache.put(routeKey, cachedRoute);

                // Also cache in Redis
                cacheRouteInRedis(routeKey, cachedRoute);
            }
        }

        log.info("Loaded {} routes from {} services", routeCache.size(), serviceCache.size());
    }

    /**
     * Register a new service with its routes using bulk upsert.
     * This method is optimized for CI/CD where services register on every startup:
     * - Uses upsert semantics (creates new, updates existing)
     * - Compares route hashes to skip unchanged routes
     * - Performs batch operations for efficiency
     */
    @Transactional
    public RegistrationResult registerService(ServiceDefinition definition) {
        log.info("Bulk upsert registration for service: {} at {}:{} with {} routes",
                definition.name(), definition.host(), definition.port(), definition.routes().size());

        // Find or create service
        ServiceRegistryEntity service = serviceRepository.findByName(definition.name())
                .orElse(ServiceRegistryEntity.builder()
                        .id(UUID.randomUUID())
                        .name(definition.name())
                        .build());

        // Update service details (always update, might have changed)
        service.setProtocol(definition.protocol() != null ? definition.protocol() : "grpc");
        service.setHost(definition.host());
        service.setPort(definition.port());
        service.setUseTls(definition.useTls());
        service.setDescriptorUrl(definition.descriptorUrl());
        service.setVersion(definition.version());
        service.setActive(true);
        service.setLastHeartbeat(LocalDateTime.now());

        int routesRegistered = 0;
        int routesSkipped = 0;

        // Get existing route hashes for this service (efficient single query)
        Map<String, String> existingHashesByPath = routeRepository.findByServiceName(definition.name())
                .stream()
                .collect(Collectors.toMap(
                        r -> r.getHttpMethod() + ":" + r.getPath(),
                        RouteRegistryEntity::getRouteHash,
                        (a, b) -> a // In case of duplicates, keep first
                ));

        // Batch process routes
        List<RouteRegistryEntity> routesToSave = new ArrayList<>();
        
        for (RouteDefinitionDto routeDef : definition.routes()) {
            String routeKey = routeDef.httpMethod() + ":" + routeDef.path();
            String newHash = computeRouteHash(routeDef);
            String existingHash = existingHashesByPath.get(routeKey);

            // Skip if route exists with same hash (unchanged)
            if (newHash.equals(existingHash)) {
                routesSkipped++;
                continue;
            }

            // Find or create route
            Optional<RouteRegistryEntity> existingRoute = routeRepository
                    .findByHttpMethodAndPath(routeDef.httpMethod(), routeDef.path());

            RouteRegistryEntity route;
            if (existingRoute.isPresent()) {
                route = existingRoute.get();
                // Update existing route
                updateRouteFromDefinition(route, routeDef, newHash);
                log.debug("Updating route: {} {}", routeDef.httpMethod(), routeDef.path());
            } else {
                // Create new route
                route = createRouteFromDefinition(routeDef, newHash);
                service.addRoute(route);
                log.debug("Creating route: {} {}", routeDef.httpMethod(), routeDef.path());
            }
            
            routesToSave.add(route);
            routesRegistered++;
        }

        // Save service (cascades to new routes)
        service = serviceRepository.save(service);
        
        // Batch save updated routes that were already attached to other services
        if (!routesToSave.isEmpty()) {
            routeRepository.saveAll(routesToSave);
        }

        // Update caches
        CachedService cachedService = toCachedService(service);
        serviceCache.put(service.getName(), cachedService);

        // Refresh all routes for this service in cache
        for (RouteRegistryEntity route : service.getRoutes()) {
            CachedRoute cachedRoute = toCachedRoute(route, cachedService);
            String cacheKey = buildRouteKey(route.getHttpMethod(), route.getPath());
            routeCache.put(cacheKey, cachedRoute);
            cacheRouteInRedis(cacheKey, cachedRoute);
        }

        if (routesRegistered > 0) {
            log.info("✅ Service {} registered: {} routes added/updated, {} unchanged",
                    definition.name(), routesRegistered, routesSkipped);
        } else {
            log.info("✅ Service {} checked in: all {} routes unchanged",
                    definition.name(), routesSkipped);
        }

        return new RegistrationResult(true, "Service registered successfully",
                routesRegistered, routesSkipped, service.getId().toString());
    }

    /**
     * Unregister a service and remove all its routes
     */
    @Transactional
    public boolean unregisterService(String serviceName) {
        Optional<ServiceRegistryEntity> serviceOpt = serviceRepository.findByName(serviceName);
        if (serviceOpt.isEmpty()) {
            return false;
        }

        ServiceRegistryEntity service = serviceOpt.get();

        // Remove routes from cache
        for (RouteRegistryEntity route : service.getRoutes()) {
            String routeKey = buildRouteKey(route.getHttpMethod(), route.getPath());
            routeCache.remove(routeKey);
            removeRouteFromRedis(routeKey);
        }

        // Remove service from cache
        serviceCache.remove(serviceName);

        // Delete from database
        serviceRepository.delete(service);

        log.info("Service {} unregistered", serviceName);
        return true;
    }

    /**
     * Update heartbeat for a service
     */
    @Transactional
    public boolean updateHeartbeat(String serviceName) {
        int updated = serviceRepository.updateHeartbeat(serviceName, LocalDateTime.now());
        if (updated > 0) {
            CachedService cachedService = serviceCache.get(serviceName);
            if (cachedService != null) {
                // Update the cached service with new heartbeat
                serviceCache.put(serviceName, new CachedService(
                        cachedService.name(),
                        cachedService.host(),
                        cachedService.port(),
                        cachedService.useTls(),
                        LocalDateTime.now()
                ));
            }
            return true;
        }
        return false;
    }

    /**
     * Resolve a route by HTTP method and path
     */
    public Optional<CachedRoute> resolveRoute(String httpMethod, String path) {
        String routeKey = buildRouteKey(httpMethod, path);

        // First, check memory cache
        CachedRoute route = routeCache.get(routeKey);
        if (route != null) {
            return Optional.of(route);
        }

        // Try pattern matching for dynamic paths
        for (Map.Entry<String, CachedRoute> entry : routeCache.entrySet()) {
            if (matchesPattern(entry.getKey(), routeKey)) {
                return Optional.of(entry.getValue());
            }
        }

        // Try Redis cache
        route = getRouteFromRedis(routeKey);
        if (route != null) {
            routeCache.put(routeKey, route);
            return Optional.of(route);
        }

        return Optional.empty();
    }

    /**
     * Get service by name
     */
    public Optional<CachedService> getService(String serviceName) {
        return Optional.ofNullable(serviceCache.get(serviceName));
    }

    /**
     * Get all registered services
     */
    public Collection<CachedService> getAllServices() {
        return serviceCache.values();
    }

    /**
     * Get all registered routes
     */
    public Collection<CachedRoute> getAllRoutes() {
        return routeCache.values();
    }

    /**
     * Check which routes need registration
     */
    public RouteCheckResult checkRoutes(String serviceName, List<RouteHashCheck> routeChecks) {
        Set<String> existingHashes = new HashSet<>(routeRepository.findRouteHashesByServiceName(serviceName));

        List<RouteHashCheck> toRegister = new ArrayList<>();
        List<RouteHashCheck> upToDate = new ArrayList<>();

        for (RouteHashCheck check : routeChecks) {
            if (existingHashes.contains(check.routeHash())) {
                upToDate.add(check);
            } else {
                toRegister.add(check);
            }
        }

        return new RouteCheckResult(toRegister, upToDate);
    }

    /**
     * Scheduled task to deactivate stale services (missed heartbeats).
     * Optimized: Runs every 5 minutes instead of every minute.
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes (was 60s - too frequent)
    @Transactional
    public void cleanupStaleServices() {
        try {
            LocalDateTime threshold = LocalDateTime.now().minus(HEARTBEAT_TIMEOUT);
            
            // First deactivate in DB (single query, no N+1)
            int deactivated = serviceRepository.deactivateStaleServices(threshold);
            
            if (deactivated > 0) {
                log.info("Deactivated {} stale services, refreshing cache...", deactivated);
                // Reload routes from DB to sync caches
                loadRoutesFromDatabase();
            }
        } catch (Exception e) {
            log.warn("Error during stale service cleanup: {}", e.getMessage());
            // Don't rethrow - scheduled task should continue
        }
    }

    // ==================== Helper Methods ====================

    private String buildRouteKey(String method, String path) {
        return method.toUpperCase() + ":" + path;
    }

    private boolean matchesPattern(String pattern, String path) {
        // Pattern and path are in format "METHOD:path"
        String[] patternParts = pattern.split(":", 2);
        String[] pathParts = path.split(":", 2);

        if (patternParts.length != 2 || pathParts.length != 2) {
            return false;
        }

        // HTTP methods must match
        if (!patternParts[0].equalsIgnoreCase(pathParts[0])) {
            return false;
        }

        String patternPath = patternParts[1];
        String actualPath = pathParts[1];

        // Use AntPathMatcher which handles {id} path variables and ** wildcards
        return pathMatcher.match(patternPath, actualPath);
    }

    /**
     * Compute a hash of the route definition for change detection.
     * NOTE: request_type and response_type are NO LONGER included in the hash
     * because they are now optional - the gateway uses reflection to discover types.
     */
    private String computeRouteHash(RouteDefinitionDto routeDef) {
        try {
            // Only include essential routing information in the hash
            // request_type and response_type are optional (discovered via reflection)
            String content = routeDef.httpMethod() + ":" + routeDef.path() + ":" +
                    routeDef.grpcService() + ":" + routeDef.grpcMethod() + ":" +
                    routeDef.publicEndpoint();

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

    private void updateRouteFromDefinition(RouteRegistryEntity route, RouteDefinitionDto def, String hash) {
        route.setGrpcService(def.grpcService());
        route.setGrpcMethod(def.grpcMethod());
        route.setRequestType(def.requestType());
        route.setResponseType(def.responseType());
        route.setPublicEndpoint(def.publicEndpoint());
        route.setRequiredRolesList(def.requiredRoles());
        route.setRouteHash(hash);
    }

    private RouteRegistryEntity createRouteFromDefinition(RouteDefinitionDto def, String hash) {
        RouteRegistryEntity route = RouteRegistryEntity.builder()
                .id(UUID.randomUUID())
                .httpMethod(def.httpMethod())
                .path(def.path())
                .grpcService(def.grpcService())
                .grpcMethod(def.grpcMethod())
                .requestType(def.requestType())
                .responseType(def.responseType())
                .publicEndpoint(def.publicEndpoint())
                .routeHash(hash)
                .build();
        route.setRequiredRolesList(def.requiredRoles());
        return route;
    }

    private CachedService toCachedService(ServiceRegistryEntity entity) {
        return new CachedService(
                entity.getName(),
                entity.getHost(),
                entity.getPort(),
                entity.isUseTls(),
                entity.getLastHeartbeat()
        );
    }

    private CachedRoute toCachedRoute(RouteRegistryEntity entity, CachedService service) {
        return new CachedRoute(
                service.name(),
                entity.getGrpcService(),
                entity.getGrpcMethod(),
                entity.getHttpMethod(),
                entity.getPath(),
                entity.getRequestType(),
                entity.getResponseType(),
                entity.isPublicEndpoint(),
                entity.getRequiredRolesList(),
                service
        );
    }

    private void cacheRouteInRedis(String routeKey, CachedRoute route) {
        try {
            String json = objectMapper.writeValueAsString(route);
            redisTemplate.opsForValue().set(REDIS_ROUTE_KEY_PREFIX + routeKey, json, REDIS_TTL);
            redisTemplate.opsForSet().add(REDIS_ROUTES_SET, routeKey);
        } catch (JsonProcessingException e) {
            log.warn("Failed to cache route in Redis: {}", e.getMessage());
        }
    }

    private CachedRoute getRouteFromRedis(String routeKey) {
        try {
            String json = redisTemplate.opsForValue().get(REDIS_ROUTE_KEY_PREFIX + routeKey);
            if (json != null) {
                return objectMapper.readValue(json, CachedRoute.class);
            }
        } catch (Exception e) {
            log.warn("Failed to get route from Redis: {}", e.getMessage());
        }
        return null;
    }

    private void removeRouteFromRedis(String routeKey) {
        redisTemplate.delete(REDIS_ROUTE_KEY_PREFIX + routeKey);
        redisTemplate.opsForSet().remove(REDIS_ROUTES_SET, routeKey);
    }

    // ==================== DTOs ====================

    public record ServiceDefinition(
            String name,
            String protocol,
            String host,
            int port,
            boolean useTls,
            String descriptorUrl,
            String version,
            List<RouteDefinitionDto> routes
    ) {}

    public record RouteDefinitionDto(
            String httpMethod,
            String path,
            String grpcService,
            String grpcMethod,
            String requestType,
            String responseType,
            boolean publicEndpoint,
            List<String> requiredRoles
    ) {}

    public record CachedService(
            String name,
            String host,
            int port,
            boolean useTls,
            LocalDateTime lastHeartbeat
    ) {}

    public record CachedRoute(
            String serviceName,
            String grpcServiceName,
            String grpcMethodName,
            String httpMethod,
            String path,
            String requestType,
            String responseType,
            boolean publicEndpoint,
            List<String> requiredRoles,
            CachedService service
    ) {}

    public record RegistrationResult(
            boolean success,
            String message,
            int routesRegistered,
            int routesSkipped,
            String serviceId
    ) {}

    public record RouteHashCheck(
            String httpMethod,
            String path,
            String routeHash
    ) {}

    public record RouteCheckResult(
            List<RouteHashCheck> routesToRegister,
            List<RouteHashCheck> routesUpToDate
    ) {}
}


