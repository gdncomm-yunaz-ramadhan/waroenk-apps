package com.gdn.project.waroenk.member.configuration;

import com.gdn.project.waroenk.contract.GatewayRegistrationClient;
import com.gdn.project.waroenk.contract.GatewayRegistrationClient.Route;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration to register member service with the API Gateway.
 * 
 * Features:
 * - Registration is ENABLED by default (CI/CD friendly)
 * - Async and non-blocking - won't affect service startup
 * - Uses upsert semantics - always registers, creates or updates routes
 * - Automatic retry with exponential backoff if gateway unavailable
 * 
 * Disable by setting: gateway.registration.enabled=false
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "gateway.registration.enabled", havingValue = "true", matchIfMissing = true)
public class GatewayRegistrationConfig {

    private static final String SERVICE_NAME = "member";

    @Value("${gateway.host:api-gateway}")
    private String gatewayHost;

    @Value("${gateway.grpc.port:6565}")
    private int gatewayPort;

    @Value("${grpc.server.address:0.0.0.0}")
    private String serviceHost;

    @Value("${grpc.server.port:9090}")
    private int servicePort;

    private GatewayRegistrationClient registrationClient;

    @PostConstruct
    public void registerWithGateway() {
        log.info("🚀 Starting gateway registration for service: {} -> {}:{}", SERVICE_NAME, serviceHost, servicePort);

        registrationClient = new GatewayRegistrationClient(gatewayHost, gatewayPort);

        List<Route> routes = buildRoutes();

        // Async registration - won't block startup
        registrationClient.registerAsync(SERVICE_NAME, getServiceHostForGateway(), servicePort, false, routes)
                .thenAccept(success -> {
                    if (success) {
                        log.info("✅ Service {} registered with gateway successfully ({} routes)", SERVICE_NAME, routes.size());
                    } else {
                        log.warn("⚠️ Service {} gateway registration pending (will retry)", SERVICE_NAME);
                    }
                })
                .exceptionally(ex -> {
                    log.warn("⚠️ Gateway registration failed (service will continue): {}", ex.getMessage());
                    return null;
                });
    }

    @PreDestroy
    public void unregisterFromGateway() {
        if (registrationClient != null) {
            log.info("🛑 Unregistering service {} from gateway", SERVICE_NAME);
            registrationClient.unregister();
        }
    }

    private String getServiceHostForGateway() {
        // In Docker, use service name; locally use the configured host
        if ("0.0.0.0".equals(serviceHost)) {
            return SERVICE_NAME; // Docker service name
        }
        return serviceHost;
    }

    private List<Route> buildRoutes() {
        return List.of(
                // ==================== User Service ====================
                new Route("POST", "/api/user/register",
                        "member.user.UserService", "Register",
                        "com.gdn.project.waroenk.member.CreateUserRequest",
                        "com.gdn.project.waroenk.member.CreateUserResponse",
                        true, List.of()), // Public

                new Route("POST", "/api/user/login",
                        "member.user.UserService", "Authenticate",
                        "com.gdn.project.waroenk.member.AuthenticateRequest",
                        "com.gdn.project.waroenk.member.UserTokenResponse",
                        true, List.of()), // Public

                new Route("GET", "/api/user",
                        "member.user.UserService", "GetOneUserById",
                        "com.gdn.project.waroenk.common.Id",
                        "com.gdn.project.waroenk.member.UserData",
                        false, List.of()),

                new Route("GET", "/api/user/find-one",
                        "member.user.UserService", "GetOneUserByPhoneOrEmail",
                        "com.gdn.project.waroenk.member.PhoneOrEmailRequest",
                        "com.gdn.project.waroenk.member.UserData",
                        false, List.of()),

                new Route("GET", "/api/user/filter",
                        "member.user.UserService", "FilterUser",
                        "com.gdn.project.waroenk.member.FilterUserRequest",
                        "com.gdn.project.waroenk.member.MultipleUserResponse",
                        false, List.of()),

                new Route("PUT", "/api/user",
                        "member.user.UserService", "UpdateUser",
                        "com.gdn.project.waroenk.member.UpdateUserRequest",
                        "com.gdn.project.waroenk.member.UserData",
                        false, List.of()),

                new Route("POST", "/api/user/forgot-password",
                        "member.user.UserService", "ForgotPassword",
                        "com.gdn.project.waroenk.member.ForgotPasswordRequest",
                        "com.gdn.project.waroenk.member.ForgotPasswordResponse",
                        true, List.of()), // Public

                new Route("POST", "/api/user/change-password",
                        "member.user.UserService", "ChangePassword",
                        "com.gdn.project.waroenk.member.ChangePasswordRequest",
                        "com.gdn.project.waroenk.member.ChangePasswordResponse",
                        true, List.of()), // Public (uses reset token)

                new Route("POST", "/api/user/logout",
                        "member.user.UserService", "Logout",
                        "com.gdn.project.waroenk.member.LogoutRequest",
                        "com.gdn.project.waroenk.member.LogoutResponse",
                        false, List.of()),

                new Route("POST", "/api/user/refresh-token",
                        "member.user.UserService", "RefreshToken",
                        "com.gdn.project.waroenk.member.RefreshTokenRequest",
                        "com.gdn.project.waroenk.member.RefreshTokenResponse",
                        true, List.of()), // Public (uses refresh token)

                // ==================== Address Service ====================
                new Route("POST", "/api/address",
                        "member.address.AddressService", "UpsertAddress",
                        "com.gdn.project.waroenk.member.UpsertAddressRequest",
                        "com.gdn.project.waroenk.member.AddressData",
                        false, List.of()),

                new Route("GET", "/api/address",
                        "member.address.AddressService", "FindAddressById",
                        "com.gdn.project.waroenk.common.Id",
                        "com.gdn.project.waroenk.member.AddressData",
                        false, List.of()),

                new Route("GET", "/api/address/find-one",
                        "member.address.AddressService", "FindOneUserAddressByLabel",
                        "com.gdn.project.waroenk.member.FindUserAddressRequest",
                        "com.gdn.project.waroenk.member.AddressData",
                        false, List.of()),

                new Route("GET", "/api/address/filter",
                        "member.address.AddressService", "FilterUserAddress",
                        "com.gdn.project.waroenk.member.FilterAddressRequest",
                        "com.gdn.project.waroenk.member.MultipleAddressResponse",
                        false, List.of()),

                new Route("PUT", "/api/address/default",
                        "member.address.AddressService", "SetUserDefaultAddress",
                        "com.gdn.project.waroenk.member.SetDefaultAddressRequest",
                        "com.gdn.project.waroenk.common.Basic",
                        false, List.of()),

                new Route("DELETE", "/api/address",
                        "member.address.AddressService", "DeleteAddressById",
                        "com.gdn.project.waroenk.common.Id",
                        "com.gdn.project.waroenk.common.Basic",
                        false, List.of())
        );
    }
}


