package com.gdn.project.waroenk.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for the API Gateway.
 * Defines all route mappings and service endpoints.
 * 
 * Example configuration:
 * gateway:
 *   services:
 *     member:
 *       host: localhost
 *       port: 9090
 *     catalog:
 *       host: localhost
 *       port: 9091
 *   routes:
 *     - path: /api/user/**
 *       service: member
 *       grpc-service: member.user.UserService
 *       methods:
 *         - http-method: POST
 *           http-path: /api/user/register
 *           grpc-method: Register
 *         - http-method: GET
 *           http-path: /api/user
 *           grpc-method: GetOneUserById
 *       public: false
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    /**
     * Map of service name to service configuration
     */
    private Map<String, ServiceConfig> services = new HashMap<>();

    /**
     * List of route configurations
     */
    private List<RouteConfig> routes = new ArrayList<>();

    /**
     * JWT configuration
     */
    private JwtConfig jwt = new JwtConfig();

    /**
     * Public paths that don't require authentication
     */
    private List<String> publicPaths = new ArrayList<>();

    @Data
    public static class ServiceConfig {
        private String host;
        private int port;          // gRPC port
        private int httpPort = 0;  // HTTP/Actuator port (for monitoring)
        private boolean useTls = false;
    }

    @Data
    public static class RouteConfig {
        /**
         * HTTP path pattern (e.g., /api/user/**)
         */
        private String path;

        /**
         * Target service name (must match a key in services map)
         */
        private String service;

        /**
         * Full gRPC service name (e.g., member.user.UserService)
         */
        private String grpcService;

        /**
         * Whether this route is public (no auth required)
         */
        private boolean publicRoute = false;

        /**
         * Method mappings for this route
         */
        private List<MethodMapping> methods = new ArrayList<>();
    }

    @Data
    public static class MethodMapping {
        /**
         * HTTP method (GET, POST, PUT, DELETE, PATCH)
         */
        private String httpMethod;

        /**
         * HTTP path (e.g., /api/user/register)
         */
        private String httpPath;

        /**
         * gRPC method name (e.g., Register)
         */
        private String grpcMethod;

        /**
         * Request message type (fully qualified proto class name)
         */
        private String requestType;

        /**
         * Response message type (fully qualified proto class name)
         */
        private String responseType;

        /**
         * Whether this specific endpoint is public
         */
        private boolean publicEndpoint = false;

        /**
         * Required roles for this endpoint (empty = any authenticated user)
         */
        private List<String> requiredRoles = new ArrayList<>();
    }

    @Data
    public static class JwtConfig {
        private String secret;
        private long accessTokenExpiration = 3600; // 1 hour
        private long refreshTokenExpiration = 604800; // 7 days
    }
}







