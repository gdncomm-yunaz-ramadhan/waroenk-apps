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
 * Migration V001: Initialize Cart Service Collections
 * 
 * Creates the following collections and indexes:
 * - cart_items: Main cart storage
 *   - userId (unique)
 *   - items.sku (for searching items)
 * - checkout_items: Checkout/reservation storage
 *   - checkoutId (unique)
 *   - userId
 *   - expiresAt (TTL index for auto-expiration)
 * - system_parameters: Configuration storage
 *   - variable (unique)
 */
@Slf4j
@ChangeLog(order = "001")
public class V001_InitializeCartCollections {

    @ChangeSet(order = "001", id = "createCartItemsIndexes", author = "system")
    public void createCartItemsIndexes(MongockTemplate mongockTemplate) {
        log.info("Creating indexes for cart_items collection...");
        
        IndexOperations indexOps = mongockTemplate.indexOps("cart_items");
        
        // Create unique index on userId
        try {
            indexOps.ensureIndex(new Index()
                    .on("userId", Sort.Direction.ASC)
                    .unique()
                    .named("idx_cart_userId"));
            log.info("Created unique index on cart_items.userId");
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("IndexOptionsConflict")) {
                log.info("Index on cart_items.userId already exists, skipping");
            } else {
                throw e;
            }
        }
        
        // Create index on items.sku for searching
        try {
            indexOps.ensureIndex(new Index()
                    .on("items.sku", Sort.Direction.ASC)
                    .named("idx_cart_items_sku"));
            log.info("Created index on cart_items.items.sku");
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("IndexOptionsConflict")) {
                log.info("Index on cart_items.items.sku already exists, skipping");
            } else {
                throw e;
            }
        }
        
        // Create index on updatedAt for sorting
        try {
            indexOps.ensureIndex(new Index()
                    .on("updatedAt", Sort.Direction.DESC)
                    .named("idx_cart_updatedAt"));
            log.info("Created index on cart_items.updatedAt");
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("IndexOptionsConflict")) {
                log.info("Index on cart_items.updatedAt already exists, skipping");
            } else {
                throw e;
            }
        }
        
        log.info("Cart items indexes created successfully");
    }

    @ChangeSet(order = "002", id = "createCheckoutItemsIndexes", author = "system")
    public void createCheckoutItemsIndexes(MongockTemplate mongockTemplate) {
        log.info("Creating indexes for checkout_items collection...");
        
        IndexOperations indexOps = mongockTemplate.indexOps("checkout_items");
        
        // Create unique index on checkoutId
        try {
            indexOps.ensureIndex(new Index()
                    .on("checkoutId", Sort.Direction.ASC)
                    .unique()
                    .named("idx_checkout_checkoutId"));
            log.info("Created unique index on checkout_items.checkoutId");
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("IndexOptionsConflict")) {
                log.info("Index on checkout_items.checkoutId already exists, skipping");
            } else {
                throw e;
            }
        }
        
        // Create index on userId
        try {
            indexOps.ensureIndex(new Index()
                    .on("userId", Sort.Direction.ASC)
                    .named("idx_checkout_userId"));
            log.info("Created index on checkout_items.userId");
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("IndexOptionsConflict")) {
                log.info("Index on checkout_items.userId already exists, skipping");
            } else {
                throw e;
            }
        }
        
        // Create index on status
        try {
            indexOps.ensureIndex(new Index()
                    .on("status", Sort.Direction.ASC)
                    .named("idx_checkout_status"));
            log.info("Created index on checkout_items.status");
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("IndexOptionsConflict")) {
                log.info("Index on checkout_items.status already exists, skipping");
            } else {
                throw e;
            }
        }
        
        log.info("Checkout items indexes created successfully");
    }

    @ChangeSet(order = "003", id = "createSystemParametersIndexes", author = "system")
    public void createSystemParametersIndexes(MongockTemplate mongockTemplate) {
        log.info("Creating indexes for system_parameters collection...");
        
        IndexOperations indexOps = mongockTemplate.indexOps("system_parameters");
        
        // Create unique index on variable
        try {
            indexOps.ensureIndex(new Index()
                    .on("variable", Sort.Direction.ASC)
                    .unique()
                    .named("idx_sysparam_variable"));
            log.info("Created unique index on system_parameters.variable");
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("IndexOptionsConflict")) {
                log.info("Index on system_parameters.variable already exists, skipping");
            } else {
                throw e;
            }
        }
        
        log.info("System parameters indexes created successfully");
    }

    @ChangeSet(order = "004", id = "seedDefaultSystemParameters", author = "system")
    public void seedDefaultSystemParameters(MongockTemplate mongockTemplate) {
        log.info("Seeding default system parameters...");
        
        // Check if default parameters already exist
        long count = mongockTemplate.getCollection("system_parameters").countDocuments();
        if (count > 0) {
            log.info("System parameters already exist, skipping seed");
            return;
        }
        
        // Insert default checkout TTL parameter
        org.bson.Document checkoutTtl = new org.bson.Document()
                .append("variable", "cart.checkout.ttl-seconds")
                .append("data", "900")
                .append("description", "TTL for checkout items in seconds (default: 15 minutes)")
                .append("type", "INTEGER")
                .append("createdAt", java.time.Instant.now())
                .append("updatedAt", java.time.Instant.now());
        mongockTemplate.getCollection("system_parameters").insertOne(checkoutTtl);
        
        // Insert default max items per cart parameter
        org.bson.Document maxItems = new org.bson.Document()
                .append("variable", "cart.max-items")
                .append("data", "100")
                .append("description", "Maximum number of unique items per cart")
                .append("type", "INTEGER")
                .append("createdAt", java.time.Instant.now())
                .append("updatedAt", java.time.Instant.now());
        mongockTemplate.getCollection("system_parameters").insertOne(maxItems);
        
        // Insert default max quantity per item parameter
        org.bson.Document maxQuantity = new org.bson.Document()
                .append("variable", "cart.max-quantity-per-item")
                .append("data", "999")
                .append("description", "Maximum quantity per item in cart")
                .append("type", "INTEGER")
                .append("createdAt", java.time.Instant.now())
                .append("updatedAt", java.time.Instant.now());
        mongockTemplate.getCollection("system_parameters").insertOne(maxQuantity);
        
        // Insert checkout use redis parameter
        org.bson.Document useRedis = new org.bson.Document()
                .append("variable", "cart.checkout.use-redis")
                .append("data", "true")
                .append("description", "Whether to use Redis for checkout storage")
                .append("type", "BOOLEAN")
                .append("createdAt", java.time.Instant.now())
                .append("updatedAt", java.time.Instant.now());
        mongockTemplate.getCollection("system_parameters").insertOne(useRedis);
        
        log.info("Default system parameters seeded successfully");
    }
}




