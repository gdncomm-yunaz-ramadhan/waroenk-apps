package com.gdn.project.waroenk.cart.migration;

import com.github.cloudyrock.mongock.ChangeLog;
import com.github.cloudyrock.mongock.ChangeSet;
import com.github.cloudyrock.mongock.driver.mongodb.springdata.v3.decorator.impl.MongockTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

/**
 * Migration V002: Checkout System Enhancements
 * 
 * Adds indexes and fields to support the checkout-plan.md requirements:
 * 
 * Cart Collection Updates:
 * - items.subSku index for inventory lookup
 * 
 * Checkout Collection Updates:
 * - orderId index for searching by order
 * - expiresAt index for expiry cleanup (remove TTL, use manual cleanup)
 * - Compound index (userId, createdAt DESC) for cursor pagination
 * - status index for filtering
 * 
 * New fields (handled at entity level):
 * - CartItem: subSku, availableStockSnapshot
 * - CheckoutItem: subSku, availableStockSnapshot, imageUrl, attributes, reservationError
 * - Checkout: orderId, paymentCode, shippingAddress, currency, paidAt, cancelledAt
 */
@Slf4j
@ChangeLog(order = "002")
public class V002_CheckoutSystemEnhancements {

    @ChangeSet(order = "001", id = "addCartSubSkuIndex", author = "system")
    public void addCartSubSkuIndex(MongockTemplate mongockTemplate) {
        log.info("Adding subSku index for cart_items collection...");
        
        IndexOperations indexOps = mongockTemplate.indexOps("cart_items");
        
        // Create index on items.subSku for inventory lookup
        try {
            indexOps.ensureIndex(new Index()
                    .on("items.subSku", Sort.Direction.ASC)
                    .named("idx_cart_items_subSku"));
            log.info("Created index on cart_items.items.subSku");
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("IndexOptionsConflict")) {
                log.info("Index on cart_items.items.subSku already exists, skipping");
            } else {
                throw e;
            }
        }
    }

    @ChangeSet(order = "002", id = "addCheckoutOrderIdIndex", author = "system")
    public void addCheckoutOrderIdIndex(MongockTemplate mongockTemplate) {
        log.info("Adding orderId index for checkout_items collection...");
        
        IndexOperations indexOps = mongockTemplate.indexOps("checkout_items");
        
        // Create index on orderId for searching by order
        try {
            indexOps.ensureIndex(new Index()
                    .on("orderId", Sort.Direction.ASC)
                    .sparse()
                    .named("idx_checkout_orderId"));
            log.info("Created sparse index on checkout_items.orderId");
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("IndexOptionsConflict")) {
                log.info("Index on checkout_items.orderId already exists, skipping");
            } else {
                throw e;
            }
        }
    }

    @ChangeSet(order = "003", id = "addCheckoutExpiresAtIndex", author = "system")
    public void addCheckoutExpiresAtIndex(MongockTemplate mongockTemplate) {
        log.info("Migrating expiresAt index for checkout_items collection...");
        
        IndexOperations indexOps = mongockTemplate.indexOps("checkout_items");
        
        // First, try to drop the old TTL index if it exists
        // The old index was created with @Indexed(expireAfterSeconds = 0) which causes conflicts
        try {
            // Try dropping by the auto-generated name (just "expiresAt")
            indexOps.dropIndex("expiresAt");
            log.info("Dropped old TTL index 'expiresAt'");
        } catch (Exception e) {
            log.info("Old index 'expiresAt' not found or already dropped: {}", e.getMessage());
        }
        
        // Create new regular index on expiresAt for scheduled expiry cleanup
        // Note: This is NOT a TTL index - we handle expiry manually to release inventory
        try {
            indexOps.ensureIndex(new Index()
                    .on("expiresAt", Sort.Direction.ASC)
                    .named("idx_checkout_expiresAt"));
            log.info("Created index on checkout_items.expiresAt");
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("IndexOptionsConflict")) {
                log.info("Index on checkout_items.expiresAt already exists, skipping");
            } else {
                throw e;
            }
        }
    }

    @ChangeSet(order = "004", id = "addCheckoutUserCreatedCompoundIndex", author = "system")
    public void addCheckoutUserCreatedCompoundIndex(MongockTemplate mongockTemplate) {
        log.info("Adding compound index (userId, createdAt DESC) for checkout_items collection...");
        
        IndexOperations indexOps = mongockTemplate.indexOps("checkout_items");
        
        // Create compound index for cursor-based pagination
        try {
            indexOps.ensureIndex(new Index()
                    .on("userId", Sort.Direction.ASC)
                    .on("createdAt", Sort.Direction.DESC)
                    .named("idx_checkout_user_created"));
            log.info("Created compound index on checkout_items.(userId, createdAt)");
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("IndexOptionsConflict")) {
                log.info("Compound index on checkout_items.(userId, createdAt) already exists, skipping");
            } else {
                throw e;
            }
        }
    }

    @ChangeSet(order = "005", id = "addCheckoutPaymentCodeIndex", author = "system")
    public void addCheckoutPaymentCodeIndex(MongockTemplate mongockTemplate) {
        log.info("Adding paymentCode index for checkout_items collection...");
        
        IndexOperations indexOps = mongockTemplate.indexOps("checkout_items");
        
        // Create unique sparse index on paymentCode
        try {
            indexOps.ensureIndex(new Index()
                    .on("paymentCode", Sort.Direction.ASC)
                    .unique()
                    .sparse()
                    .named("idx_checkout_paymentCode"));
            log.info("Created unique sparse index on checkout_items.paymentCode");
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("IndexOptionsConflict")) {
                log.info("Index on checkout_items.paymentCode already exists, skipping");
            } else {
                throw e;
            }
        }
    }

    @ChangeSet(order = "006", id = "seedCheckoutSystemParameters", author = "system")
    public void seedCheckoutSystemParameters(MongockTemplate mongockTemplate) {
        log.info("Seeding checkout-related system parameters...");
        
        // Insert checkout expiry check interval parameter
        insertSystemParameterIfNotExists(mongockTemplate, 
                "cart.checkout.expiry-check-interval-seconds",
                "60",
                "Interval between expiry checks in seconds",
                "INTEGER");

        // Insert checkout payment code prefix
        insertSystemParameterIfNotExists(mongockTemplate,
                "cart.checkout.payment-code-prefix",
                "PAY",
                "Prefix for generated payment codes",
                "STRING");

        // Insert checkout order ID prefix
        insertSystemParameterIfNotExists(mongockTemplate,
                "cart.checkout.order-id-prefix",
                "ORD",
                "Prefix for generated order IDs",
                "STRING");

        log.info("Checkout system parameters seeded successfully");
    }

    private void insertSystemParameterIfNotExists(MongockTemplate mongockTemplate, 
            String variable, String data, String description, String type) {
        org.bson.Document existing = mongockTemplate.getCollection("system_parameters")
                .find(new org.bson.Document("variable", variable))
                .first();
        
        if (existing == null) {
            org.bson.Document doc = new org.bson.Document()
                    .append("variable", variable)
                    .append("data", data)
                    .append("description", description)
                    .append("type", type)
                    .append("createdAt", java.time.Instant.now())
                    .append("updatedAt", java.time.Instant.now());
            mongockTemplate.getCollection("system_parameters").insertOne(doc);
            log.info("Inserted system parameter: {}", variable);
        } else {
            log.info("System parameter {} already exists, skipping", variable);
        }
    }
}

