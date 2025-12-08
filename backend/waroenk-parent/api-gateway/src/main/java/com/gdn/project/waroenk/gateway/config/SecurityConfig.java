package com.gdn.project.waroenk.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdn.project.waroenk.gateway.dto.ErrorResponseDto;
import com.gdn.project.waroenk.gateway.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final GatewayProperties gatewayProperties;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> 
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .authorizeHttpRequests(auth -> {
                    // Allow actuator endpoints
                    auth.requestMatchers("/actuator/**").permitAll();
                    // Allow Swagger/OpenAPI endpoints (include .html explicitly)
                    auth.requestMatchers(
                            "/swagger-ui.html",
                            "/swagger-ui/**", 
                            "/api-docs", 
                            "/api-docs/**", 
                            "/v3/api-docs",
                            "/v3/api-docs/**"
                    ).permitAll();
                    // Allow monitoring dashboard endpoints
                    auth.requestMatchers("/dashboard", "/dashboard/**", "/monitoring/**").permitAll();
                    
                    // Allow configured public paths
                    for (String publicPath : gatewayProperties.getPublicPaths()) {
                        auth.requestMatchers(publicPath).permitAll();
                    }
                    
                    // Allow public routes from route configuration
                    for (GatewayProperties.RouteConfig route : gatewayProperties.getRoutes()) {
                        if (route.isPublicRoute()) {
                            auth.requestMatchers(route.getPath()).permitAll();
                        }
                        for (GatewayProperties.MethodMapping method : route.getMethods()) {
                            if (method.isPublicEndpoint()) {
                                auth.requestMatchers(method.getHttpPath()).permitAll();
                            }
                        }
                    }
                    
                    // All other requests require authentication
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Handle authentication errors (401 Unauthorized)
     * Returns JSON error response for unauthenticated requests
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            
            ErrorResponseDto errorResponse = new ErrorResponseDto(
                    HttpStatus.UNAUTHORIZED.value(),
                    HttpStatus.UNAUTHORIZED.name(),
                    "Authentication required. Please provide a valid token.",
                    LocalDateTime.now()
            );
            
            objectMapper.writeValue(response.getOutputStream(), errorResponse);
        };
    }

    /**
     * Handle authorization errors (403 Forbidden)
     * Returns JSON error response for unauthorized requests
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            
            ErrorResponseDto errorResponse = new ErrorResponseDto(
                    HttpStatus.FORBIDDEN.value(),
                    HttpStatus.FORBIDDEN.name(),
                    "Access denied. You don't have permission to access this resource.",
                    LocalDateTime.now()
            );
            
            objectMapper.writeValue(response.getOutputStream(), errorResponse);
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}






