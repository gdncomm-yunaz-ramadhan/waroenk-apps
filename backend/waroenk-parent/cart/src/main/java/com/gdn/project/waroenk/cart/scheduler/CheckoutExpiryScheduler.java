package com.gdn.project.waroenk.cart.scheduler;

import com.gdn.project.waroenk.cart.client.CatalogGrpcClient;
import com.gdn.project.waroenk.cart.entity.Checkout;
import com.gdn.project.waroenk.cart.entity.CheckoutItem;
import com.gdn.project.waroenk.cart.repository.CheckoutRepository;
import com.gdn.project.waroenk.cart.service.SystemParameterService;
import com.gdn.project.waroenk.catalog.BulkReleaseStockResponse;
import com.gdn.project.waroenk.catalog.StockOperationItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Scheduled task to handle checkout expiry.
 * 
 * Runs periodically to:
 * 1. Find checkouts where status = WAITING and expiresAt < now()
 * 2. Release locked inventory via catalog gRPC
 * 3. Mark checkout as CANCELLED
 * 
 * This ensures inventory is not locked indefinitely when users abandon checkouts.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "cart.checkout.expiry-scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class CheckoutExpiryScheduler {

    private final CheckoutRepository checkoutRepository;
    private final MongoTemplate mongoTemplate;
    private final CatalogGrpcClient catalogClient;
    private final SystemParameterService systemParameterService;

    /**
     * Run expiry check every minute (configurable via cron expression).
     * Can be overridden via property: cart.checkout.expiry-scheduler.cron
     */
    @Scheduled(cron = "${cart.checkout.expiry-scheduler.cron:0 * * * * *}")
    public void processExpiredCheckouts() {
        log.debug("Running checkout expiry check...");
        
        try {
            // Find all expired checkouts
            Query query = new Query(Criteria.where("status").is("WAITING")
                    .and("expiresAt").lt(Instant.now()));
            
            List<Checkout> expiredCheckouts = mongoTemplate.find(query, Checkout.class);
            
            if (expiredCheckouts.isEmpty()) {
                log.debug("No expired checkouts found");
                return;
            }
            
            log.info("Found {} expired checkouts to process", expiredCheckouts.size());
            
            int successCount = 0;
            int failureCount = 0;
            
            for (Checkout checkout : expiredCheckouts) {
                try {
                    processExpiredCheckout(checkout);
                    successCount++;
                } catch (Exception e) {
                    log.error("Failed to process expired checkout {}: {}", 
                            checkout.getCheckoutId(), e.getMessage());
                    failureCount++;
                }
            }
            
            log.info("Expiry processing complete. Success: {}, Failed: {}", successCount, failureCount);
            
        } catch (Exception e) {
            log.error("Error in checkout expiry scheduler: {}", e.getMessage(), e);
        }
    }

    /**
     * Process a single expired checkout:
     * 1. Release inventory locks via gRPC
     * 2. Update status to CANCELLED
     */
    private void processExpiredCheckout(Checkout checkout) {
        log.info("Processing expired checkout: {}", checkout.getCheckoutId());
        
        // Release inventory locks for reserved items
        List<CheckoutItem> reservedItems = checkout.getItems().stream()
                .filter(item -> Boolean.TRUE.equals(item.getReserved()))
                .collect(Collectors.toList());
        
        if (!reservedItems.isEmpty()) {
            List<StockOperationItem> releaseItems = reservedItems.stream()
                    .map(item -> StockOperationItem.newBuilder()
                            .setSubSku(item.getSubSku())
                            .setQuantity(item.getQuantity())
                            .build())
                    .collect(Collectors.toList());
            
            BulkReleaseStockResponse response = catalogClient.bulkReleaseStock(
                    checkout.getCheckoutId(), releaseItems);
            
            if (response.getAllSuccess()) {
                log.info("Released inventory for checkout {}: {} items", 
                        checkout.getCheckoutId(), releaseItems.size());
            } else {
                log.warn("Partial inventory release for checkout {}: success={}, failed={}", 
                        checkout.getCheckoutId(), 
                        response.getSuccessCount(), 
                        response.getFailureCount());
            }
        }
        
        // Update checkout status
        checkout.setStatus("CANCELLED");
        checkout.setCancelledAt(Instant.now());
        checkoutRepository.save(checkout);
        
        log.info("Marked checkout {} as CANCELLED (expired)", checkout.getCheckoutId());
    }

    /**
     * Manual trigger for expiry processing (useful for testing or admin operations).
     * @return Number of checkouts processed
     */
    public int triggerExpiryProcessing() {
        log.info("Manual expiry processing triggered");
        
        Query query = new Query(Criteria.where("status").is("WAITING")
                .and("expiresAt").lt(Instant.now()));
        
        List<Checkout> expiredCheckouts = mongoTemplate.find(query, Checkout.class);
        
        int processed = 0;
        for (Checkout checkout : expiredCheckouts) {
            try {
                processExpiredCheckout(checkout);
                processed++;
            } catch (Exception e) {
                log.error("Failed to process checkout {}: {}", checkout.getCheckoutId(), e.getMessage());
            }
        }
        
        return processed;
    }
}







