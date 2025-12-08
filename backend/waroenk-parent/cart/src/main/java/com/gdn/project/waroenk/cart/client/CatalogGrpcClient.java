package com.gdn.project.waroenk.cart.client;

import com.gdn.project.waroenk.catalog.*;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * gRPC client for communicating with the Catalog microservice.
 * Handles inventory operations (stock check, lock, release, acquire)
 * and variant data retrieval.
 */
@Slf4j
@Component
public class CatalogGrpcClient {

    @GrpcClient("catalog-service")
    private InventoryServiceGrpc.InventoryServiceBlockingStub inventoryStub;

    @GrpcClient("catalog-service")
    private VariantServiceGrpc.VariantServiceBlockingStub variantStub;

    // ============================================================
    // Inventory Operations
    // ============================================================

    /**
     * Get inventory data by sub-SKU
     */
    public Optional<InventoryData> getInventoryBySubSku(String subSku) {
        try {
            FindInventoryBySubSkuRequest request = FindInventoryBySubSkuRequest.newBuilder()
                    .setSubSku(subSku)
                    .build();
            InventoryData response = inventoryStub.findInventoryBySubSku(request);
            return Optional.of(response);
        } catch (StatusRuntimeException e) {
            log.warn("Failed to get inventory for subSku {}: {}", subSku, e.getStatus());
            return Optional.empty();
        }
    }

    /**
     * Bulk lock stock for checkout
     */
    public BulkLockStockResponse bulkLockStock(String checkoutId, List<StockOperationItem> items, int ttlSeconds) {
        try {
            BulkLockStockRequest request = BulkLockStockRequest.newBuilder()
                    .setCheckoutId(checkoutId)
                    .addAllItems(items)
                    .setLockTtlSeconds(ttlSeconds)
                    .build();
            return inventoryStub.bulkLockStock(request);
        } catch (StatusRuntimeException e) {
            log.error("Failed to bulk lock stock for checkout {}: {}", checkoutId, e.getStatus());
            // Return a failure response
            return BulkLockStockResponse.newBuilder()
                    .setCheckoutId(checkoutId)
                    .setAllSuccess(false)
                    .setFailureCount(items.size())
                    .build();
        }
    }

    /**
     * Bulk acquire stock (confirm reservation and deduct stock permanently)
     */
    public BulkAcquireStockResponse bulkAcquireStock(String checkoutId, List<StockOperationItem> items) {
        try {
            BulkAcquireStockRequest request = BulkAcquireStockRequest.newBuilder()
                    .setCheckoutId(checkoutId)
                    .addAllItems(items)
                    .build();
            return inventoryStub.bulkAcquireStock(request);
        } catch (StatusRuntimeException e) {
            log.error("Failed to bulk acquire stock for checkout {}: {}", checkoutId, e.getStatus());
            return BulkAcquireStockResponse.newBuilder()
                    .setCheckoutId(checkoutId)
                    .setAllSuccess(false)
                    .setFailureCount(items.size())
                    .build();
        }
    }

    /**
     * Bulk release stock (return reserved stock)
     */
    public BulkReleaseStockResponse bulkReleaseStock(String checkoutId, List<StockOperationItem> items) {
        try {
            BulkReleaseStockRequest request = BulkReleaseStockRequest.newBuilder()
                    .setCheckoutId(checkoutId)
                    .addAllItems(items)
                    .build();
            return inventoryStub.bulkReleaseStock(request);
        } catch (StatusRuntimeException e) {
            log.error("Failed to bulk release stock for checkout {}: {}", checkoutId, e.getStatus());
            return BulkReleaseStockResponse.newBuilder()
                    .setCheckoutId(checkoutId)
                    .setAllSuccess(false)
                    .setFailureCount(items.size())
                    .build();
        }
    }

    /**
     * Check stock availability for a single sub-SKU
     */
    public long getAvailableStock(String subSku) {
        return getInventoryBySubSku(subSku)
                .map(InventoryData::getStock)
                .orElse(0L);
    }

    // ============================================================
    // Variant Operations
    // ============================================================

    /**
     * Get variant data by sub-SKU
     */
    public Optional<VariantData> getVariantBySubSku(String subSku) {
        try {
            FindVariantBySubSkuRequest request = FindVariantBySubSkuRequest.newBuilder()
                    .setSubSku(subSku)
                    .build();
            VariantData response = variantStub.findVariantBySubSku(request);
            return Optional.of(response);
        } catch (StatusRuntimeException e) {
            log.warn("Failed to get variant for subSku {}: {}", subSku, e.getStatus());
            return Optional.empty();
        }
    }

    /**
     * Get variant data by ID
     */
    public Optional<VariantData> getVariantById(String variantId) {
        try {
            com.gdn.project.waroenk.common.Id request = com.gdn.project.waroenk.common.Id.newBuilder()
                    .setValue(variantId)
                    .build();
            VariantData response = variantStub.findVariantById(request);
            return Optional.of(response);
        } catch (StatusRuntimeException e) {
            log.warn("Failed to get variant by id {}: {}", variantId, e.getStatus());
            return Optional.empty();
        }
    }

    /**
     * Get all variants for a product SKU
     */
    public MultipleVariantResponse getVariantsBySku(String sku, int size, String cursor) {
        try {
            FindVariantsBySkuRequest.Builder requestBuilder = FindVariantsBySkuRequest.newBuilder()
                    .setSku(sku)
                    .setSize(size);
            if (cursor != null && !cursor.isEmpty()) {
                requestBuilder.setCursor(cursor);
            }
            return variantStub.findVariantsBySku(requestBuilder.build());
        } catch (StatusRuntimeException e) {
            log.warn("Failed to get variants for sku {}: {}", sku, e.getStatus());
            return MultipleVariantResponse.getDefaultInstance();
        }
    }

    // ============================================================
    // Combined Operations (for cart convenience)
    // ============================================================

    /**
     * Get variant and inventory data together for cart operations.
     * Returns a combined result with variant info and stock.
     */
    public Optional<VariantWithStock> getVariantWithStock(String subSku) {
        Optional<VariantData> variant = getVariantBySubSku(subSku);
        if (variant.isEmpty()) {
            return Optional.empty();
        }
        
        long stock = getAvailableStock(subSku);
        
        return Optional.of(new VariantWithStock(variant.get(), stock));
    }

    /**
     * Combined variant and stock data
     */
    public record VariantWithStock(VariantData variant, long availableStock) {
        public String getSku() {
            return variant.getSku();
        }

        public String getSubSku() {
            return variant.getSubSku();
        }

        public String getTitle() {
            return variant.getTitle();
        }

        public double getPrice() {
            return variant.getPrice();
        }

        public String getThumbnail() {
            return variant.getThumbnail();
        }

        public boolean hasStock(int quantity) {
            return availableStock >= quantity;
        }
    }
}

