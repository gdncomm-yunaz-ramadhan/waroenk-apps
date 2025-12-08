package com.gdn.project.waroenk.gateway.controller;

import com.gdn.project.waroenk.gateway.service.GrpcProxyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Dynamic gateway controller that routes all /api/** requests to gRPC services.
 * The routing is determined by configuration, not hardcoded endpoints.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Gateway", description = "API Gateway that routes HTTP requests to gRPC microservices")
public class GatewayController {

    private final GrpcProxyService grpcProxyService;

    @GetMapping("/api/**")
    @Operation(summary = "Handle GET requests", description = "Routes GET requests to the appropriate gRPC service")
    public ResponseEntity<String> handleGet(HttpServletRequest request) {
        return handleRequest(request, null);
    }

    @PostMapping(value = "/api/**", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Handle POST requests", description = "Routes POST requests to the appropriate gRPC service")
    public ResponseEntity<String> handlePost(HttpServletRequest request, @RequestBody(required = false) String body) {
        return handleRequest(request, body);
    }

    @PutMapping(value = "/api/**", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Handle PUT requests", description = "Routes PUT requests to the appropriate gRPC service")
    public ResponseEntity<String> handlePut(HttpServletRequest request, @RequestBody(required = false) String body) {
        return handleRequest(request, body);
    }

    @PatchMapping(value = "/api/**", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Handle PATCH requests", description = "Routes PATCH requests to the appropriate gRPC service")
    public ResponseEntity<String> handlePatch(HttpServletRequest request, @RequestBody(required = false) String body) {
        return handleRequest(request, body);
    }

    @DeleteMapping("/api/**")
    @Operation(summary = "Handle DELETE requests", description = "Routes DELETE requests to the appropriate gRPC service")
    public ResponseEntity<String> handleDelete(HttpServletRequest request) {
        return handleRequest(request, null);
    }

    private ResponseEntity<String> handleRequest(HttpServletRequest request, String body) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        Map<String, String> queryParams = extractQueryParams(request);
        
        // Extract authenticated user context from JWT filter
        String userId = (String) request.getAttribute("userId");
        String username = (String) request.getAttribute("username");

        // Use debug level for per-request logging to reduce overhead
        log.debug("Gateway request: {} {} params={} userId={}", method, path, queryParams, userId);

        try {
            String response = grpcProxyService.invoke(method, path, body, queryParams, userId, username);
            
            // Return raw response without double-processing
            // The gRPC response is already valid JSON
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
                    
        } catch (Exception e) {
            log.warn("Gateway error: {} {} - {}", method, path, e.getMessage());
            throw e; // Let the ControllerAdvice handle it
        }
    }

    private Map<String, String> extractQueryParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> paramNames = request.getParameterNames();
        
        while (paramNames.hasMoreElements()) {
            String name = paramNames.nextElement();
            params.put(name, request.getParameter(name));
        }
        
        return params;
    }
}


