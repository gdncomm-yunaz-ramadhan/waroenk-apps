package com.gdn.project.waroenk.gateway.security;

import java.util.List;

/**
 * Principal object containing authenticated user information
 */
public record UserPrincipal(
        String userId,
        String username,
        List<String> roles
) {
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean hasAnyRole(List<String> requiredRoles) {
        if (requiredRoles == null || requiredRoles.isEmpty()) {
            return true;
        }
        return roles != null && requiredRoles.stream().anyMatch(roles::contains);
    }
}










