package com.gdn.project.waroenk.catalog.configuration;

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
 * Configuration to register catalog service with the API Gateway.
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

    private static final String SERVICE_NAME = "catalog";

    @Value("${gateway.host:api-gateway}")
    private String gatewayHost;

    @Value("${gateway.grpc.port:6565}")
    private int gatewayPort;

    @Value("${grpc.server.address:0.0.0.0}")
    private String serviceHost;

    @Value("${grpc.server.port:9091}")
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
                // ==================== Product Service ====================
                new Route("POST", "/api/product",
                        "catalog.product.ProductService", "CreateProduct",
                        "com.gdn.project.waroenk.catalog.CreateProductRequest",
                        "com.gdn.project.waroenk.catalog.ProductData",
                        false, List.of()),

                new Route("PUT", "/api/product",
                        "catalog.product.ProductService", "UpdateProduct",
                        "com.gdn.project.waroenk.catalog.UpdateProductRequest",
                        "com.gdn.project.waroenk.catalog.ProductData",
                        false, List.of()),

                new Route("DELETE", "/api/product",
                        "catalog.product.ProductService", "DeleteProduct",
                        "com.gdn.project.waroenk.common.Id",
                        "com.gdn.project.waroenk.common.Basic",
                        false, List.of()),

                new Route("GET", "/api/product",
                        "catalog.product.ProductService", "FindProductById",
                        "com.gdn.project.waroenk.common.Id",
                        "com.gdn.project.waroenk.catalog.ProductData",
                        true, List.of()), // Public

                new Route("GET", "/api/product/by-sku",
                        "catalog.product.ProductService", "FindProductBySku",
                        "com.gdn.project.waroenk.catalog.FindProductBySkuRequest",
                        "com.gdn.project.waroenk.catalog.ProductData",
                        true, List.of()), // Public

                new Route("GET", "/api/product/filter",
                        "catalog.product.ProductService", "FilterProduct",
                        "com.gdn.project.waroenk.catalog.FilterProductRequest",
                        "com.gdn.project.waroenk.catalog.MultipleProductResponse",
                        true, List.of()), // Public

                new Route("GET", "/api/product/details",
                        "catalog.search.SearchService", "GetProductDetails",
                        "com.gdn.project.waroenk.catalog.GetProductDetailsRequest",
                        "com.gdn.project.waroenk.catalog.GetProductDetailsResponse",
                        true, List.of()), // Public

                new Route("POST", "/api/product/summary",
                        "catalog.search.SearchService", "GetProductSummary",
                        "com.gdn.project.waroenk.catalog.GetProductSummaryRequest",
                        "com.gdn.project.waroenk.catalog.GetProductSummaryResponse",
                        true, List.of()), // Public

                // ==================== Inventory Check ====================
                new Route("POST", "/api/inventory/check",
                        "catalog.search.SearchService", "CheckInventory",
                        "com.gdn.project.waroenk.catalog.CheckInventoryRequest",
                        "com.gdn.project.waroenk.catalog.CheckInventoryResponse",
                        true, List.of()), // Public

                // ==================== Brand Service ====================
                new Route("POST", "/api/brand",
                        "catalog.brand.BrandService", "CreateBrand",
                        "com.gdn.project.waroenk.catalog.CreateBrandRequest",
                        "com.gdn.project.waroenk.catalog.BrandData",
                        false, List.of()),

                new Route("PUT", "/api/brand",
                        "catalog.brand.BrandService", "UpdateBrand",
                        "com.gdn.project.waroenk.catalog.UpdateBrandRequest",
                        "com.gdn.project.waroenk.catalog.BrandData",
                        false, List.of()),

                new Route("DELETE", "/api/brand",
                        "catalog.brand.BrandService", "DeleteBrand",
                        "com.gdn.project.waroenk.common.Id",
                        "com.gdn.project.waroenk.common.Basic",
                        false, List.of()),

                new Route("GET", "/api/brand",
                        "catalog.brand.BrandService", "FindBrandById",
                        "com.gdn.project.waroenk.common.Id",
                        "com.gdn.project.waroenk.catalog.BrandData",
                        true, List.of()), // Public

                new Route("GET", "/api/brand/by-slug",
                        "catalog.brand.BrandService", "FindBrandBySlug",
                        "com.gdn.project.waroenk.catalog.FindBrandBySlugRequest",
                        "com.gdn.project.waroenk.catalog.BrandData",
                        true, List.of()), // Public

                new Route("GET", "/api/brand/filter",
                        "catalog.brand.BrandService", "FilterBrand",
                        "com.gdn.project.waroenk.catalog.FilterBrandRequest",
                        "com.gdn.project.waroenk.catalog.MultipleBrandResponse",
                        true, List.of()), // Public

                // ==================== Search Service ====================
                new Route("GET", "/api/search",
                        "catalog.search.SearchService", "Search",
                        "com.gdn.project.waroenk.catalog.CombinedSearchRequest",
                        "com.gdn.project.waroenk.catalog.CombinedSearchResponse",
                        true, List.of()), // Public

                new Route("GET", "/api/search/products",
                        "catalog.search.SearchService", "SearchProducts",
                        "com.gdn.project.waroenk.catalog.SearchProductsRequest",
                        "com.gdn.project.waroenk.catalog.SearchProductsResponse",
                        true, List.of()), // Public

                new Route("GET", "/api/search/merchants",
                        "catalog.search.SearchService", "SearchMerchants",
                        "com.gdn.project.waroenk.catalog.SearchMerchantsRequest",
                        "com.gdn.project.waroenk.catalog.SearchMerchantsResponse",
                        true, List.of()), // Public

                // ==================== Merchant Service ====================
                new Route("POST", "/api/merchant",
                        "catalog.merchant.MerchantService", "CreateMerchant",
                        "com.gdn.project.waroenk.catalog.CreateMerchantRequest",
                        "com.gdn.project.waroenk.catalog.MerchantData",
                        false, List.of()),

                new Route("PUT", "/api/merchant",
                        "catalog.merchant.MerchantService", "UpdateMerchant",
                        "com.gdn.project.waroenk.catalog.UpdateMerchantRequest",
                        "com.gdn.project.waroenk.catalog.MerchantData",
                        false, List.of()),

                new Route("DELETE", "/api/merchant",
                        "catalog.merchant.MerchantService", "DeleteMerchant",
                        "com.gdn.project.waroenk.common.Id",
                        "com.gdn.project.waroenk.common.Basic",
                        false, List.of()),

                new Route("GET", "/api/merchant",
                        "catalog.merchant.MerchantService", "FindMerchantById",
                        "com.gdn.project.waroenk.common.Id",
                        "com.gdn.project.waroenk.catalog.MerchantData",
                        true, List.of()), // Public

                new Route("GET", "/api/merchant/by-code",
                        "catalog.merchant.MerchantService", "FindMerchantByCode",
                        "com.gdn.project.waroenk.catalog.FindMerchantByCodeRequest",
                        "com.gdn.project.waroenk.catalog.MerchantData",
                        true, List.of()), // Public

                new Route("GET", "/api/merchant/filter",
                        "catalog.merchant.MerchantService", "FilterMerchant",
                        "com.gdn.project.waroenk.catalog.FilterMerchantRequest",
                        "com.gdn.project.waroenk.catalog.MultipleMerchantResponse",
                        true, List.of()), // Public

                // ==================== Category Service ====================
                new Route("POST", "/api/category",
                        "catalog.category.CategoryService", "CreateCategory",
                        "com.gdn.project.waroenk.catalog.CreateCategoryRequest",
                        "com.gdn.project.waroenk.catalog.CategoryData",
                        false, List.of()),

                new Route("PUT", "/api/category",
                        "catalog.category.CategoryService", "UpdateCategory",
                        "com.gdn.project.waroenk.catalog.UpdateCategoryRequest",
                        "com.gdn.project.waroenk.catalog.CategoryData",
                        false, List.of()),

                new Route("DELETE", "/api/category",
                        "catalog.category.CategoryService", "DeleteCategory",
                        "com.gdn.project.waroenk.common.Id",
                        "com.gdn.project.waroenk.common.Basic",
                        false, List.of()),

                new Route("GET", "/api/category",
                        "catalog.category.CategoryService", "FindCategoryById",
                        "com.gdn.project.waroenk.common.Id",
                        "com.gdn.project.waroenk.catalog.CategoryData",
                        true, List.of()), // Public

                new Route("GET", "/api/category/by-slug",
                        "catalog.category.CategoryService", "FindCategoryBySlug",
                        "com.gdn.project.waroenk.catalog.FindCategoryBySlugRequest",
                        "com.gdn.project.waroenk.catalog.CategoryData",
                        true, List.of()), // Public

                new Route("GET", "/api/category/filter",
                        "catalog.category.CategoryService", "FilterCategory",
                        "com.gdn.project.waroenk.catalog.FilterCategoryRequest",
                        "com.gdn.project.waroenk.catalog.MultipleCategoryResponse",
                        true, List.of()), // Public

                new Route("GET", "/api/category/tree",
                        "catalog.category.CategoryService", "GetCategoryTree",
                        "com.gdn.project.waroenk.common.Empty",
                        "com.gdn.project.waroenk.catalog.CategoryTreeResponse",
                        true, List.of()), // Public

                // ==================== Variant Service ====================
                new Route("POST", "/api/variant",
                        "catalog.variant.VariantService", "CreateVariant",
                        "com.gdn.project.waroenk.catalog.CreateVariantRequest",
                        "com.gdn.project.waroenk.catalog.VariantData",
                        false, List.of()),

                new Route("PUT", "/api/variant",
                        "catalog.variant.VariantService", "UpdateVariant",
                        "com.gdn.project.waroenk.catalog.UpdateVariantRequest",
                        "com.gdn.project.waroenk.catalog.VariantData",
                        false, List.of()),

                new Route("DELETE", "/api/variant",
                        "catalog.variant.VariantService", "DeleteVariant",
                        "com.gdn.project.waroenk.common.Id",
                        "com.gdn.project.waroenk.common.Basic",
                        false, List.of()),

                new Route("GET", "/api/variant",
                        "catalog.variant.VariantService", "FindVariantById",
                        "com.gdn.project.waroenk.common.Id",
                        "com.gdn.project.waroenk.catalog.VariantData",
                        true, List.of()), // Public

                new Route("GET", "/api/variant/by-sub-sku",
                        "catalog.variant.VariantService", "FindVariantBySubSku",
                        "com.gdn.project.waroenk.catalog.FindVariantBySubSkuRequest",
                        "com.gdn.project.waroenk.catalog.VariantData",
                        true, List.of()), // Public

                new Route("GET", "/api/variant/by-sku",
                        "catalog.variant.VariantService", "FindVariantsBySku",
                        "com.gdn.project.waroenk.catalog.FindVariantsBySkuRequest",
                        "com.gdn.project.waroenk.catalog.MultipleVariantResponse",
                        true, List.of()), // Public

                new Route("GET", "/api/variant/filter",
                        "catalog.variant.VariantService", "FilterVariant",
                        "com.gdn.project.waroenk.catalog.FilterVariantRequest",
                        "com.gdn.project.waroenk.catalog.MultipleVariantResponse",
                        true, List.of()), // Public

                new Route("PUT", "/api/variant/default",
                        "catalog.variant.VariantService", "SetDefaultVariant",
                        "com.gdn.project.waroenk.catalog.SetDefaultVariantRequest",
                        "com.gdn.project.waroenk.catalog.VariantData",
                        false, List.of()) // Admin only
        );
    }
}


