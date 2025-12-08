package com.gdn.project.waroenk.cart.configuration;

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
 * Configuration to register cart service with the API Gateway.
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

    private static final String SERVICE_NAME = "cart";

    @Value("${gateway.host:api-gateway}")
    private String gatewayHost;

    @Value("${gateway.grpc.port:6565}")
    private int gatewayPort;

    @Value("${grpc.server.address:0.0.0.0}")
    private String serviceHost;

    @Value("${grpc.server.port:9092}")
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
                // ==================== Cart Service ====================
                new Route("GET", "/api/cart/{user_id}",
                        "cart.cart.CartService", "GetCart",
                        "com.gdn.project.waroenk.cart.GetCartRequest",
                        "com.gdn.project.waroenk.cart.CartData",
                        false, List.of()),

                new Route("POST", "/api/cart/add",
                        "cart.cart.CartService", "AddItem",
                        "com.gdn.project.waroenk.cart.AddCartItemRequest",
                        "com.gdn.project.waroenk.cart.AddCartItemResponse",
                        false, List.of()),

                new Route("POST", "/api/cart/bulk-add",
                        "cart.cart.CartService", "BulkAddItems",
                        "com.gdn.project.waroenk.cart.BulkAddCartItemsRequest",
                        "com.gdn.project.waroenk.cart.BulkAddCartItemsResponse",
                        false, List.of()),

                new Route("POST", "/api/cart/remove",
                        "cart.cart.CartService", "RemoveItem",
                        "com.gdn.project.waroenk.cart.RemoveCartItemRequest",
                        "com.gdn.project.waroenk.cart.CartData",
                        false, List.of()),

                new Route("POST", "/api/cart/bulk-remove",
                        "cart.cart.CartService", "BulkRemoveItems",
                        "com.gdn.project.waroenk.cart.BulkRemoveCartItemsRequest",
                        "com.gdn.project.waroenk.cart.CartData",
                        false, List.of()),

                new Route("PUT", "/api/cart/update",
                        "cart.cart.CartService", "UpdateItem",
                        "com.gdn.project.waroenk.cart.UpdateCartItemRequest",
                        "com.gdn.project.waroenk.cart.AddCartItemResponse",
                        false, List.of()),

                new Route("DELETE", "/api/cart/{user_id}",
                        "cart.cart.CartService", "ClearCart",
                        "com.gdn.project.waroenk.cart.ClearCartRequest",
                        "com.gdn.project.waroenk.common.Basic",
                        false, List.of()),

                new Route("GET", "/api/cart/filter",
                        "cart.cart.CartService", "FilterCarts",
                        "com.gdn.project.waroenk.cart.FilterCartRequest",
                        "com.gdn.project.waroenk.cart.MultipleCartResponse",
                        false, List.of("ADMIN")), // Admin only

                // ==================== Checkout Service ====================
                // Main Checkout Flow
                new Route("POST", "/api/checkout/prepare",
                        "cart.checkout.CheckoutService", "PrepareCheckout",
                        "com.gdn.project.waroenk.cart.PrepareCheckoutRequest",
                        "com.gdn.project.waroenk.cart.PrepareCheckoutResponse",
                        false, List.of()),

                new Route("POST", "/api/checkout/{checkout_id}/finalize",
                        "cart.checkout.CheckoutService", "FinalizeCheckout",
                        "com.gdn.project.waroenk.cart.FinalizeCheckoutRequest",
                        "com.gdn.project.waroenk.cart.FinalizeCheckoutResponse",
                        false, List.of()),

                new Route("POST", "/api/checkout/{checkout_id}/pay",
                        "cart.checkout.CheckoutService", "PayCheckout",
                        "com.gdn.project.waroenk.cart.PayCheckoutRequest",
                        "com.gdn.project.waroenk.cart.PayCheckoutResponse",
                        false, List.of()),

                new Route("POST", "/api/checkout/{checkout_id}/cancel",
                        "cart.checkout.CheckoutService", "CancelCheckout",
                        "com.gdn.project.waroenk.cart.CancelCheckoutRequest",
                        "com.gdn.project.waroenk.cart.CancelCheckoutResponse",
                        false, List.of()),

                // Retrieval APIs
                new Route("GET", "/api/checkout/{checkout_id}",
                        "cart.checkout.CheckoutService", "GetCheckout",
                        "com.gdn.project.waroenk.cart.GetCheckoutRequest",
                        "com.gdn.project.waroenk.cart.CheckoutData",
                        false, List.of()),

                new Route("GET", "/api/checkout/user/{user_id}",
                        "cart.checkout.CheckoutService", "GetCheckoutByUser",
                        "com.gdn.project.waroenk.cart.GetCheckoutByUserRequest",
                        "com.gdn.project.waroenk.cart.CheckoutData",
                        false, List.of()),

                new Route("GET", "/api/checkouts",
                        "cart.checkout.CheckoutService", "FilterCheckouts",
                        "com.gdn.project.waroenk.cart.FilterCheckoutRequest",
                        "com.gdn.project.waroenk.cart.MultipleCheckoutResponse",
                        false, List.of()),

                // Legacy Support (Deprecated - for backward compatibility)
                new Route("POST", "/api/checkout/validate",
                        "cart.checkout.CheckoutService", "ValidateAndReserve",
                        "com.gdn.project.waroenk.cart.ValidateCheckoutRequest",
                        "com.gdn.project.waroenk.cart.ValidateCheckoutResponse",
                        false, List.of()),

                new Route("POST", "/api/checkout/invalidate",
                        "cart.checkout.CheckoutService", "InvalidateCheckout",
                        "com.gdn.project.waroenk.cart.InvalidateCheckoutRequest",
                        "com.gdn.project.waroenk.common.Basic",
                        false, List.of())
        );
    }
}


