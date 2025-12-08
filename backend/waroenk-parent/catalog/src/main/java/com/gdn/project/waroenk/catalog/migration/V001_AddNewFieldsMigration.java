package com.gdn.project.waroenk.catalog.migration;

import com.github.cloudyrock.mongock.ChangeLog;
import com.github.cloudyrock.mongock.ChangeSet;
import com.github.cloudyrock.mongock.driver.mongodb.springdata.v3.decorator.impl.MongockTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * Migration V001: Add new fields to entities
 * 
 * Changes:
 * - Merchant: add location field
 * - Variant: add title, isDefault fields
 * - Add indexes for new fields
 */
@Slf4j
@ChangeLog(order = "001")
public class V001_AddNewFieldsMigration {

  @ChangeSet(order = "001", id = "addLocationToMerchants", author = "system")
  public void addLocationToMerchants(MongockTemplate mongockTemplate) {
    log.info("Adding location field to merchants...");
    
    // Try to add text index for name (for search)
    // Skip if index already exists (created by entity annotations)
    try {
      mongockTemplate.indexOps("merchants")
          .ensureIndex(TextIndexDefinition.builder()
              .onField("name")
              .build());
      log.info("Created text index on merchants.name");
    } catch (DataIntegrityViolationException e) {
      if (e.getMessage() != null && e.getMessage().contains("IndexOptionsConflict")) {
        log.info("Text index already exists on merchants collection, skipping creation");
      } else {
        throw e;
      }
    }
    
    log.info("Merchant migration completed");
  }

  @ChangeSet(order = "002", id = "addTitleAndIsDefaultToVariants", author = "system")
  public void addTitleAndIsDefaultToVariants(MongockTemplate mongockTemplate) {
    log.info("Adding title and isDefault fields to variants...");
    
    // Set default value for isDefault on all existing variants
    Query query = new Query();
    Update update = new Update()
        .set("isDefault", false);
    mongockTemplate.updateMulti(query, update, "variants");
    
    // Try to add text index for title (for search)
    try {
      mongockTemplate.indexOps("variants")
          .ensureIndex(TextIndexDefinition.builder()
              .onField("title")
              .build());
      log.info("Created text index on variants.title");
    } catch (DataIntegrityViolationException e) {
      if (e.getMessage() != null && e.getMessage().contains("IndexOptionsConflict")) {
        log.info("Text index already exists on variants collection, skipping creation");
      } else {
        throw e;
      }
    }
    
    // Try to add compound index for sku + isDefault
    try {
      mongockTemplate.indexOps("variants")
          .ensureIndex(new Index().on("sku", org.springframework.data.domain.Sort.Direction.ASC)
              .on("isDefault", org.springframework.data.domain.Sort.Direction.ASC));
      log.info("Created compound index on variants (sku, isDefault)");
    } catch (DataIntegrityViolationException e) {
      if (e.getMessage() != null && e.getMessage().contains("IndexOptionsConflict")) {
        log.info("Compound index already exists on variants collection, skipping creation");
      } else {
        throw e;
      }
    }
    
    log.info("Variant migration completed");
  }

  @ChangeSet(order = "003", id = "setFirstVariantAsDefault", author = "system")
  public void setFirstVariantAsDefault(MongockTemplate mongockTemplate) {
    log.info("Setting first variant as default for each product...");
    
    // Get all unique SKUs and set the first variant as default
    // This is done via aggregation to find the first variant for each SKU
    var aggregation = mongockTemplate.aggregate(
        org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation(
            org.springframework.data.mongodb.core.aggregation.Aggregation.group("sku")
                .first("_id").as("firstVariantId"),
            org.springframework.data.mongodb.core.aggregation.Aggregation.project("firstVariantId")
        ),
        "variants",
        org.bson.Document.class
    );

    aggregation.getMappedResults().forEach(doc -> {
      String variantId = doc.getString("firstVariantId");
      if (variantId != null) {
        Query updateQuery = Query.query(
            org.springframework.data.mongodb.core.query.Criteria.where("_id").is(variantId)
        );
        Update setDefault = new Update().set("isDefault", true);
        mongockTemplate.updateFirst(updateQuery, setDefault, "variants");
      }
    });
    
    log.info("Default variant setting completed");
  }
}
