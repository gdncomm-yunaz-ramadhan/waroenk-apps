package com.gdn.project.waroenk.gateway.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdn.project.waroenk.gateway.config.GrpcChannelConfig;
import com.gdn.project.waroenk.gateway.exception.GatewayException;
import com.gdn.project.waroenk.gateway.exception.RouteNotFoundException;
import com.gdn.project.waroenk.gateway.exception.ServiceUnavailableException;
import io.grpc.Channel;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service that dynamically invokes gRPC methods based on route configuration.
 * 
 * This is the core of the agnostic gateway - it uses gRPC Server Reflection
 * to discover service/method descriptors at runtime, eliminating the need
 * to know about proto types at compile time.
 * 
 * Key features:
 * - Uses ReflectionGrpcClient for dynamic type discovery
 * - No need to specify request/response types in route configuration
 * - Automatically injects user context from JWT into requests
 * - Supports path variables and query parameters
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrpcProxyService {

    private final ReflectionGrpcClient reflectionClient;
    private final RouteResolver routeResolver;
    private final GrpcChannelConfig channelConfig;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        // Set the route resolver on the channel config for dynamic channel creation
        channelConfig.setRouteResolver(routeResolver);
        log.info("GrpcProxyService initialized with reflection-based dynamic invocation");
    }

    /**
     * Invoke a gRPC method based on HTTP request parameters.
     * Uses gRPC Server Reflection for dynamic type discovery.
     *
     * @param httpMethod  HTTP method (GET, POST, etc.)
     * @param path        HTTP path
     * @param jsonBody    Request body as JSON (can be null for GET requests)
     * @param queryParams Query parameters (for GET requests)
     * @param userId      Authenticated user ID (from JWT, may be null for public endpoints)
     * @param username    Authenticated username (from JWT, may be null for public endpoints)
     * @return Response as JSON string
     */
    public String invoke(String httpMethod, String path, String jsonBody, Map<String, String> queryParams,
                         String userId, String username) {
        // Resolve the route
        RouteResolver.ResolvedRoute route = routeResolver.resolve(httpMethod, path)
                .orElseThrow(() -> new RouteNotFoundException(
                        "No microservice registered for " + httpMethod + " " + path));

        log.debug("Resolved route: {} {} -> {}.{}", httpMethod, path, route.grpcServiceName(), route.grpcMethodName());

        try {
            // Extract path variables from the URL using the matched pattern
            Map<String, String> pathVariables = extractPathVariables(route, path);
            
            // Merge path variables with query params (path variables take precedence)
            Map<String, String> allParams = mergeParams(queryParams, pathVariables);
            
            // Build the final JSON request with user context injection
            String finalJsonBody = buildRequestJson(route, jsonBody, allParams, userId, username);
            
            // Get channel to the target service
            Channel channel = channelConfig.getChannel(route.serviceName());
            
            // Invoke using reflection-based dynamic client
            return reflectionClient.invokeMethod(
                    channel,
                    route.grpcServiceName(),
                    route.grpcMethodName(),
                    finalJsonBody
            );

        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed: {}", e.getStatus());
            if (e.getStatus().getCode() == io.grpc.Status.Code.UNAVAILABLE) {
                throw new ServiceUnavailableException("Service " + route.serviceName() + " is unavailable");
            }
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("Service/method not found: {}", e.getMessage());
            throw new ServiceUnavailableException("Service " + route.serviceName() + " is not available: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during gRPC invocation: {}", e.getMessage(), e);
            throw new GatewayException("Failed to invoke service: " + e.getMessage(), e);
        }
    }

    /**
     * Extract path variables from the URL using the matched pattern.
     */
    private Map<String, String> extractPathVariables(RouteResolver.ResolvedRoute route, String path) {
        Map<String, String> pathVariables = new HashMap<>();
        if (route.httpPathPattern() != null) {
            try {
                pathVariables = routeResolver.extractPathVariables(route.httpPathPattern(), path);
                log.debug("Extracted path variables: {}", pathVariables);
            } catch (Exception e) {
                log.debug("No path variables to extract for pattern: {}", route.httpPathPattern());
            }
        }
        return pathVariables;
    }

    /**
     * Merge query params with path variables (path variables take precedence).
     */
    private Map<String, String> mergeParams(Map<String, String> queryParams, Map<String, String> pathVariables) {
        Map<String, String> allParams = new HashMap<>();
        if (queryParams != null) {
            allParams.putAll(queryParams);
        }
        allParams.putAll(pathVariables);
        return allParams;
    }

    /**
     * Build the final JSON request body.
     * Merges the original body with path variables/query params and injects user context.
     */
    private String buildRequestJson(RouteResolver.ResolvedRoute route, String jsonBody, 
                                    Map<String, String> queryParams, String userId, String username) {
        
        // Default to empty JSON if null
        if (jsonBody == null || jsonBody.isBlank()) {
            jsonBody = "{}";
        }

        // Merge path variables and query params into the body
        // This is critical for endpoints with path parameters like /checkout/{checkout_id}/finalize
        if (queryParams != null && !queryParams.isEmpty()) {
            jsonBody = mergeParamsIntoBody(jsonBody, queryParams);
        }

        // Inject authenticated user context into the request for non-public endpoints
        if (!route.publicEndpoint() && userId != null && !userId.isBlank()) {
            jsonBody = injectUserContext(jsonBody, userId, username);
        }

        return jsonBody;
    }

    /**
     * Merge path variables and query params into the existing JSON body.
     * Existing body values take precedence over params (to allow explicit overrides).
     */
    private String mergeParamsIntoBody(String jsonBody, Map<String, String> params) {
        try {
            Map<String, Object> bodyMap = objectMapper.readValue(jsonBody, new TypeReference<HashMap<String, Object>>() {});
            
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                
                // Only add if not already present in body (body takes precedence)
                if (!bodyMap.containsKey(key) || bodyMap.get(key) == null) {
                    // Try to detect numeric or boolean values
                    if (value.matches("-?\\d+(\\.\\d+)?")) {
                        if (value.contains(".")) {
                            bodyMap.put(key, Double.parseDouble(value));
                        } else {
                            bodyMap.put(key, Long.parseLong(value));
                        }
                    } else if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                        bodyMap.put(key, Boolean.parseBoolean(value));
                    } else {
                        bodyMap.put(key, value);
                    }
                }
            }
            
            return objectMapper.writeValueAsString(bodyMap);
        } catch (Exception e) {
            log.warn("Failed to merge params into body: {}", e.getMessage());
            return jsonBody;
        }
    }

    /**
     * Inject user context (user_id, user, value, id) from JWT into the JSON request body.
     * This ensures backend services receive the authenticated user's identity
     * without requiring the frontend to send it (security best practice).
     * 
     * We inject multiple field names to handle different proto message schemas:
     * - user_id: Always injected for security (cart, checkout, etc.)
     * - user: Always injected (FilterAddressRequest)
     * - value: Only injected if NOT already present (to allow passing resource IDs)
     * - id: Only injected if NOT already present (to allow passing resource IDs)
     */
    private String injectUserContext(String jsonBody, String userId, String username) {
        try {
            // Parse the existing JSON body into a mutable map
            Map<String, Object> bodyMap = objectMapper.readValue(jsonBody, new TypeReference<HashMap<String, Object>>() {});
            
            // Inject user_id if not already present (from JWT takes precedence for security)
            if (userId != null && !userId.isBlank()) {
                // Always override user_id from JWT for security (prevent spoofing)
                bodyMap.put("user_id", userId);
                // Also set 'user' field for requests that use that field name (e.g., FilterAddressRequest)
                bodyMap.put("user", userId);
                
                // Only set 'value' if NOT already present in the request
                // This allows endpoints like GET /api/address?value={address_id} to work
                if (!bodyMap.containsKey("value") || bodyMap.get("value") == null || bodyMap.get("value").toString().isBlank()) {
                    bodyMap.put("value", userId);
                }
                
                // Only set 'id' if NOT already present in the request
                // This allows endpoints to pass resource IDs without being overwritten
                if (!bodyMap.containsKey("id") || bodyMap.get("id") == null || bodyMap.get("id").toString().isBlank()) {
                    bodyMap.put("id", userId);
                }
            }
            
            // Optionally inject username if needed
            if (username != null && !username.isBlank()) {
                bodyMap.put("username", username);
            }
            
            // Convert back to JSON
            return objectMapper.writeValueAsString(bodyMap);
            
        } catch (Exception e) {
            log.warn("Failed to inject user context into request body: {}", e.getMessage());
            // Return original body if injection fails
            return jsonBody;
        }
    }

}
