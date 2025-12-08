package com.gdn.project.waroenk.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdn.project.waroenk.gateway.dto.ErrorResponseDto;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        final String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        // No auth header - let Spring Security handle it
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(BEARER_PREFIX.length());
            
            if (jwtUtil.validateToken(jwt)) {
                String userId = jwtUtil.extractUserId(jwt);
                String username = jwtUtil.extractUsername(jwt);
                List<String> roles = jwtUtil.extractRoles(jwt);

                // Build authorities from roles
                List<SimpleGrantedAuthority> authorities = roles != null
                        ? roles.stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                                .toList()
                        : Collections.emptyList();

                // Create authentication object
                UserPrincipal principal = new UserPrincipal(userId, username, roles);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set authentication in context
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                // Add user info to request attributes for downstream use
                request.setAttribute("userId", userId);
                request.setAttribute("username", username);
                request.setAttribute("roles", roles);
            }
            
            filterChain.doFilter(request, response);
            
        } catch (ExpiredJwtException e) {
            log.debug("JWT token expired: {}", e.getMessage());
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Token has expired");
        } catch (JwtException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Invalid token");
        } catch (Exception e) {
            log.error("Error processing JWT token: {}", e.getMessage(), e);
            sendErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "Authentication error");
        }
    }

    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) 
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                status.value(),
                status.name(),
                message,
                LocalDateTime.now()
        );
        
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip filter for actuator, swagger, and dashboard endpoints
        return path.startsWith("/actuator") || 
               path.startsWith("/swagger-ui") || 
               path.equals("/swagger-ui.html") ||
               path.startsWith("/api-docs") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/dashboard") ||
               path.startsWith("/monitoring") ||
               path.equals("/health") ||
               path.equals("/info") ||
               path.equals("/routes") ||
               path.equals("/services");
    }
}







