package com.gdn.project.waroenk.gateway.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI Configuration for API Gateway.
 * 
 * Provides two documentation views:
 * 1. Gateway Management APIs (/health, /info, /routes, /services)
 * 2. Dynamic Proxy APIs (all /api/** routes to gRPC services)
 * 
 * Access:
 * - Swagger UI: /swagger-ui.html
 * - OpenAPI JSON: /api-docs
 * - Dynamic Routes JSON: /api-docs/dynamic
 * - Route Documentation: /api-docs/routes
 */
@Configuration
public class SwaggerConfig {

    @Value("${info.app.version:1.0.0}")
    private String appVersion;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Waroenk API Gateway")
                        .description("""
                                ## Agnostic HTTP-to-gRPC API Gateway
                                
                                This gateway routes HTTP requests to backend gRPC microservices using **Server Reflection**.
                                
                                ### Key Features:
                                - **Dynamic Routing**: Routes are registered by microservices at startup
                                - **Type Agnostic**: No need to define request/response types - discovered via reflection
                                - **JWT Authentication**: Protected endpoints require Bearer token
                                
                                ### Documentation Endpoints:
                                - `/api-docs/dynamic` - OpenAPI spec for all registered routes
                                - `/api-docs/routes` - Simplified route documentation by service
                                - `/routes` - Raw route registry data
                                - `/services` - Registered microservices
                                """)
                        .version(appVersion)
                        .contact(new Contact()
                                .name("Yunaz Gilang Ramadhan")
                                .email("yunaz.ramadhan@gdn-commerce.com")))
                .externalDocs(new ExternalDocumentation()
                        .description("Dynamic API Documentation")
                        .url("/api-docs/dynamic"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT Authentication - Get token from POST /api/user/login")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    /**
     * Group for Gateway management APIs
     */
    @Bean
    public GroupedOpenApi gatewayApis() {
        return GroupedOpenApi.builder()
                .group("1-gateway-management")
                .displayName("Gateway Management")
                .pathsToMatch("/health", "/info", "/routes/**", "/services", "/api-docs/**")
                .build();
    }

    /**
     * Group for all proxied API endpoints
     */
    @Bean
    public GroupedOpenApi proxyApis() {
        return GroupedOpenApi.builder()
                .group("2-proxy-apis")
                .displayName("Proxy APIs (HTTP→gRPC)")
                .pathsToMatch("/api/**")
                .build();
    }
}







