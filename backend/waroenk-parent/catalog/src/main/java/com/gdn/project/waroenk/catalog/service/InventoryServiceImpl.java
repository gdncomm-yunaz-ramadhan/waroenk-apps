package com.gdn.project.waroenk.catalog.service;

import com.gdn.project.waroenk.catalog.AdjustStockRequest;
import com.gdn.project.waroenk.catalog.BulkAcquireStockRequest;
import com.gdn.project.waroenk.catalog.BulkAcquireStockResponse;
import com.gdn.project.waroenk.catalog.BulkAdjustStockRequest;
import com.gdn.project.waroenk.catalog.BulkAdjustStockResponse;
import com.gdn.project.waroenk.catalog.BulkLockStockRequest;
import com.gdn.project.waroenk.catalog.BulkLockStockResponse;
import com.gdn.project.waroenk.catalog.BulkReleaseStockRequest;
import com.gdn.project.waroenk.catalog.BulkReleaseStockResponse;
import com.gdn.project.waroenk.catalog.BulkUpdateStockRequest;
import com.gdn.project.waroenk.catalog.BulkUpdateStockResponse;
import com.gdn.project.waroenk.catalog.FilterInventoryRequest;
import com.gdn.project.waroenk.catalog.MultipleInventoryResponse;
import com.gdn.project.waroenk.catalog.StockOperationItem;
import com.gdn.project.waroenk.catalog.StockOperationResult;
import com.gdn.project.waroenk.catalog.UpdateStockBySubSkuRequest;
import com.gdn.project.waroenk.catalog.entity.Inventory;
import com.gdn.project.waroenk.catalog.exceptions.DuplicateResourceException;
import com.gdn.project.waroenk.catalog.exceptions.ResourceNotFoundException;
import com.gdn.project.waroenk.catalog.mapper.InventoryMapper;
import com.gdn.project.waroenk.catalog.repository.InventoryRepository;
import com.gdn.project.waroenk.catalog.repository.MongoPageAble;
import com.gdn.project.waroenk.catalog.repository.model.ResultData;
import com.gdn.project.waroenk.catalog.utility.CacheUtil;
import com.gdn.project.waroenk.catalog.utility.ParserUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class InventoryServiceImpl extends MongoPageAble<Inventory, String> implements InventoryService {
  private static final InventoryMapper mapper = InventoryMapper.INSTANCE;
  private static final String INVENTORY_PREFIX = "inventory";
  private static final String LOCK_PREFIX = "inventory:lock:";
  private static final int DEFAULT_LOCK_TTL_SECONDS = 900; // 15 minutes

  private final InventoryRepository repository;
  private final CacheUtil<Inventory> cacheUtil;
  private final CacheUtil<String> lockCacheUtil;

  @Value("${default.item-per-page:10}")
  private Integer defaultItemPerPage;

  public InventoryServiceImpl(InventoryRepository repository,
      CacheUtil<Inventory> cacheUtil,
      CacheUtil<String> stringCacheUtil,
      MongoTemplate mongoTemplate) {
    super(INVENTORY_PREFIX, stringCacheUtil, mongoTemplate, 10, TimeUnit.MINUTES, Inventory.class);
    this.repository = repository;
    this.cacheUtil = cacheUtil;
    this.lockCacheUtil = stringCacheUtil;
  }

  @Override
  public Inventory createInventory(Inventory inventory) {
    if (repository.existsBySubSku(inventory.getSubSku())) {
      throw new DuplicateResourceException("Inventory for subSku " + inventory.getSubSku() + " already exists");
    }
    Inventory saved = repository.save(inventory);
    cacheUtil.putValue(INVENTORY_PREFIX + ":" + saved.getId(), saved, 7, TimeUnit.DAYS);
    cacheUtil.putValue(INVENTORY_PREFIX + ":subSku:" + saved.getSubSku(), saved, 7, TimeUnit.DAYS);
    return saved;
  }

  @Override
  public Inventory updateInventory(String id, Inventory inventory) {
    Inventory existing = findInventoryById(id);

    if (inventory.getSubSku() != null && !existing.getSubSku().equals(inventory.getSubSku())) {
      if (repository.existsBySubSku(inventory.getSubSku())) {
        throw new DuplicateResourceException("Inventory for subSku " + inventory.getSubSku() + " already exists");
      }
      cacheUtil.removeValue(INVENTORY_PREFIX + ":subSku:" + existing.getSubSku());
      existing.setSubSku(inventory.getSubSku());
    }
    if (inventory.getStock() != null) existing.setStock(inventory.getStock());

    Inventory updated = repository.save(existing);
    cacheUtil.putValue(INVENTORY_PREFIX + ":" + updated.getId(), updated, 7, TimeUnit.DAYS);
    cacheUtil.putValue(INVENTORY_PREFIX + ":subSku:" + updated.getSubSku(), updated, 7, TimeUnit.DAYS);
    return updated;
  }

  @Override
  public Inventory updateStockBySubSku(String subSku, Long stock) {
    Inventory existing = findInventoryBySubSku(subSku);
    existing.setStock(stock);
    Inventory updated = repository.save(existing);
    cacheUtil.putValue(INVENTORY_PREFIX + ":" + updated.getId(), updated, 7, TimeUnit.DAYS);
    cacheUtil.putValue(INVENTORY_PREFIX + ":subSku:" + updated.getSubSku(), updated, 7, TimeUnit.DAYS);
    return updated;
  }

  @Override
  public Inventory adjustStock(String subSku, Long quantity) {
    Inventory existing = findInventoryBySubSku(subSku);
    Long newStock = existing.getStock() + quantity;
    if (newStock < 0) {
      throw new IllegalArgumentException("Stock cannot be negative. Current: " + existing.getStock() + ", Adjustment: " + quantity);
    }
    existing.setStock(newStock);
    Inventory updated = repository.save(existing);
    cacheUtil.putValue(INVENTORY_PREFIX + ":" + updated.getId(), updated, 7, TimeUnit.DAYS);
    cacheUtil.putValue(INVENTORY_PREFIX + ":subSku:" + updated.getSubSku(), updated, 7, TimeUnit.DAYS);
    return updated;
  }

  @Override
  public BulkUpdateStockResponse bulkUpdateStock(BulkUpdateStockRequest request) {
    BulkUpdateStockResponse.Builder builder = BulkUpdateStockResponse.newBuilder();
    int successCount = 0;
    int failureCount = 0;

    for (UpdateStockBySubSkuRequest item : request.getItemsList()) {
      try {
        Inventory updated = updateStockBySubSku(item.getSubSku(), item.getStock());
        builder.addData(mapper.toResponseGrpc(updated));
        successCount++;
      } catch (Exception e) {
        log.warn("Failed to update stock for subSku {}: {}", item.getSubSku(), e.getMessage());
        failureCount++;
      }
    }

    builder.setSuccessCount(successCount);
    builder.setFailureCount(failureCount);
    return builder.build();
  }

  @Override
  public Inventory findInventoryById(String id) {
    String key = INVENTORY_PREFIX + ":" + id;
    Inventory cached = cacheUtil.getValue(key);
    if (ObjectUtils.isNotEmpty(cached)) {
      return cached;
    }

    Inventory inventory = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Inventory with id " + id + " not found"));
    cacheUtil.putValue(key, inventory, 7, TimeUnit.DAYS);
    return inventory;
  }

  @Override
  public Inventory findInventoryBySubSku(String subSku) {
    String key = INVENTORY_PREFIX + ":subSku:" + subSku;
    Inventory cached = cacheUtil.getValue(key);
    if (ObjectUtils.isNotEmpty(cached)) {
      return cached;
    }

    Inventory inventory = repository.findBySubSku(subSku)
        .orElseThrow(() -> new ResourceNotFoundException("Inventory for subSku " + subSku + " not found"));
    cacheUtil.putValue(key, inventory, 7, TimeUnit.DAYS);
    return inventory;
  }

  @Override
  public boolean deleteInventory(String id) {
    Inventory existing = findInventoryById(id);
    repository.deleteById(id);
    cacheUtil.removeValue(INVENTORY_PREFIX + ":" + id);
    cacheUtil.removeValue(INVENTORY_PREFIX + ":subSku:" + existing.getSubSku());
    return true;
  }

  @Override
  public List<Inventory> findBulkInventoriesBySubSkus(List<String> subSkus) {
    if(ObjectUtils.isEmpty(subSkus)){
      return new ArrayList<>();
    }
    return this.repository.findBySubSkuIn(subSkus);
  }

  @Override
  public MultipleInventoryResponse filterInventory(FilterInventoryRequest request) {
    int size = request.getSize() > 0 ? request.getSize() : defaultItemPerPage;

    CriteriaBuilder criteriaBuilder = () -> {
      List<Criteria> criteriaList = new ArrayList<>();
      if (StringUtils.isNotBlank(request.getSubSku())) {
        criteriaList.add(Criteria.where("subSku").regex(request.getSubSku(), "i"));
      }
      if (ObjectUtils.isNotEmpty(request.getMinStock())) {
        criteriaList.add(Criteria.where("stock").gte(request.getMinStock()));
      }
      if (ObjectUtils.isNotEmpty(request.getMaxStock())) {
        criteriaList.add(Criteria.where("stock").lte(request.getMaxStock()));
      }
      return criteriaList;
    };

    ResultData<Inventory> entries = query(criteriaBuilder, size, request.getCursor(),
        mapper.toSortByDto(request.getSortBy()));
    Long total = entries.getTotal();
    String nextToken = null;
    Optional<Inventory> offset = entries.getOffset();
    if (offset.isPresent()) {
      nextToken = ParserUtil.encodeBase64(offset.get().getId());
    }

    MultipleInventoryResponse.Builder builder = MultipleInventoryResponse.newBuilder();
    entries.getDataList().iterator().forEachRemaining(item -> builder.addData(mapper.toResponseGrpc(item)));
    if (StringUtils.isNotBlank(nextToken)) {
      builder.setNextToken(nextToken);
    }
    builder.setTotal(ObjectUtils.isNotEmpty(total) ? total.intValue() : 0);
    return builder.build();
  }

  // ============================================================
  // Bulk Lock/Acquire/Release Operations for Cart/Checkout
  // ============================================================

  @Override
  public BulkLockStockResponse bulkLockStock(BulkLockStockRequest request) {
    String checkoutId = request.getCheckoutId();
    int lockTtl = request.getLockTtlSeconds() > 0 ? request.getLockTtlSeconds() : DEFAULT_LOCK_TTL_SECONDS;

    BulkLockStockResponse.Builder responseBuilder = BulkLockStockResponse.newBuilder()
        .setCheckoutId(checkoutId);

    List<StockOperationResult> results = new ArrayList<>();
    int successCount = 0;
    int failureCount = 0;

    // Pre-validate: check if all items have sufficient stock
    Map<String, Inventory> inventoryMap = new HashMap<>();
    for (StockOperationItem item : request.getItemsList()) {
      try {
        Inventory inventory = findInventoryBySubSku(item.getSubSku());
        inventoryMap.put(item.getSubSku(), inventory);
      } catch (ResourceNotFoundException e) {
        results.add(buildFailureResult(item.getSubSku(), item.getQuantity(), 0L,
            "Inventory not found for subSku: " + item.getSubSku()));
        failureCount++;
      }
    }

    // Process lock for each item
    for (StockOperationItem item : request.getItemsList()) {
      String subSku = item.getSubSku();
      long requestedQty = item.getQuantity();

      if (!inventoryMap.containsKey(subSku)) {
        continue; // Already handled above
      }

      Inventory inventory = inventoryMap.get(subSku);
      long currentStock = inventory.getStock() != null ? inventory.getStock() : 0;

      // Check for existing lock on this item for the same checkout
      String lockKey = LOCK_PREFIX + checkoutId + ":" + subSku;
      String existingLock = lockCacheUtil.getValue(lockKey);
      long existingLockQty = existingLock != null ? Long.parseLong(existingLock) : 0;

      // Calculate total locked quantity across all checkouts for this subSku
      long totalLockedQty = getTotalLockedQuantity(subSku);
      long availableStock = currentStock - totalLockedQty + existingLockQty; // Add back existing lock for this checkout

      if (availableStock < requestedQty) {
        results.add(buildFailureResult(subSku, requestedQty, currentStock,
            String.format("Insufficient stock. Available: %d, Requested: %d", availableStock, requestedQty)));
        failureCount++;
        continue;
      }

      // Create or update lock
      lockCacheUtil.putValue(lockKey, String.valueOf(requestedQty), lockTtl, TimeUnit.SECONDS);
      results.add(buildSuccessResult(subSku, requestedQty, currentStock, "Stock locked successfully"));
      successCount++;
    }

    responseBuilder.addAllResults(results);
    responseBuilder.setSuccessCount(successCount);
    responseBuilder.setFailureCount(failureCount);
    responseBuilder.setAllSuccess(failureCount == 0);

    return responseBuilder.build();
  }

  @Override
  public BulkAcquireStockResponse bulkAcquireStock(BulkAcquireStockRequest request) {
    String checkoutId = request.getCheckoutId();

    BulkAcquireStockResponse.Builder responseBuilder = BulkAcquireStockResponse.newBuilder()
        .setCheckoutId(checkoutId);

    List<StockOperationResult> results = new ArrayList<>();
    int successCount = 0;
    int failureCount = 0;

    for (StockOperationItem item : request.getItemsList()) {
      String subSku = item.getSubSku();
      long requestedQty = item.getQuantity();
      String lockKey = LOCK_PREFIX + checkoutId + ":" + subSku;

      try {
        // Verify lock exists
        String existingLock = lockCacheUtil.getValue(lockKey);
        if (existingLock == null) {
          results.add(buildFailureResult(subSku, requestedQty, 0L,
              "No lock found for this checkout and subSku. Lock may have expired."));
          failureCount++;
          continue;
        }

        // Reduce stock
        Inventory inventory = findInventoryBySubSku(subSku);
        long currentStock = inventory.getStock() != null ? inventory.getStock() : 0;
        long newStock = currentStock - requestedQty;

        if (newStock < 0) {
          results.add(buildFailureResult(subSku, requestedQty, currentStock,
              String.format("Insufficient stock. Current: %d, Requested: %d", currentStock, requestedQty)));
          failureCount++;
          continue;
        }

        inventory.setStock(newStock);
        Inventory updated = repository.save(inventory);

        // Update cache
        cacheUtil.putValue(INVENTORY_PREFIX + ":" + updated.getId(), updated, 7, TimeUnit.DAYS);
        cacheUtil.putValue(INVENTORY_PREFIX + ":subSku:" + updated.getSubSku(), updated, 7, TimeUnit.DAYS);

        // Remove lock
        lockCacheUtil.removeValue(lockKey);

        results.add(buildSuccessResult(subSku, requestedQty, newStock, "Stock acquired successfully"));
        successCount++;

      } catch (ResourceNotFoundException e) {
        results.add(buildFailureResult(subSku, requestedQty, 0L,
            "Inventory not found for subSku: " + subSku));
        failureCount++;
      } catch (Exception e) {
        log.error("Error acquiring stock for subSku {}: {}", subSku, e.getMessage(), e);
        results.add(buildFailureResult(subSku, requestedQty, 0L,
            "Error acquiring stock: " + e.getMessage()));
        failureCount++;
      }
    }

    responseBuilder.addAllResults(results);
    responseBuilder.setSuccessCount(successCount);
    responseBuilder.setFailureCount(failureCount);
    responseBuilder.setAllSuccess(failureCount == 0);

    return responseBuilder.build();
  }

  @Override
  public BulkReleaseStockResponse bulkReleaseStock(BulkReleaseStockRequest request) {
    String checkoutId = request.getCheckoutId();

    BulkReleaseStockResponse.Builder responseBuilder = BulkReleaseStockResponse.newBuilder()
        .setCheckoutId(checkoutId);

    List<StockOperationResult> results = new ArrayList<>();
    int successCount = 0;
    int failureCount = 0;

    for (StockOperationItem item : request.getItemsList()) {
      String subSku = item.getSubSku();
      long requestedQty = item.getQuantity();
      String lockKey = LOCK_PREFIX + checkoutId + ":" + subSku;

      try {
        // Check if lock exists
        String existingLock = lockCacheUtil.getValue(lockKey);
        if (existingLock == null) {
          // Lock doesn't exist or already expired - consider it a success
          results.add(buildSuccessResult(subSku, requestedQty, 0L,
              "Lock already released or expired"));
          successCount++;
          continue;
        }

        // Remove lock
        lockCacheUtil.removeValue(lockKey);

        Inventory inventory = null;
        try {
          inventory = findInventoryBySubSku(subSku);
        } catch (ResourceNotFoundException ignored) {
          // Inventory not found is OK for release
        }

        long currentStock = inventory != null && inventory.getStock() != null ? inventory.getStock() : 0;
        results.add(buildSuccessResult(subSku, requestedQty, currentStock, "Stock lock released successfully"));
        successCount++;

      } catch (Exception e) {
        log.error("Error releasing stock lock for subSku {}: {}", subSku, e.getMessage(), e);
        results.add(buildFailureResult(subSku, requestedQty, 0L,
            "Error releasing stock lock: " + e.getMessage()));
        failureCount++;
      }
    }

    responseBuilder.addAllResults(results);
    responseBuilder.setSuccessCount(successCount);
    responseBuilder.setFailureCount(failureCount);
    responseBuilder.setAllSuccess(failureCount == 0);

    return responseBuilder.build();
  }

  @Override
  public BulkAdjustStockResponse bulkAdjustStock(BulkAdjustStockRequest request) {
    BulkAdjustStockResponse.Builder responseBuilder = BulkAdjustStockResponse.newBuilder();

    List<StockOperationResult> results = new ArrayList<>();
    int successCount = 0;
    int failureCount = 0;

    for (AdjustStockRequest item : request.getItemsList()) {
      String subSku = item.getSubSku();
      long quantity = item.getQuantity();

      try {
        Inventory updated = adjustStock(subSku, quantity);
        results.add(buildSuccessResult(subSku, Math.abs(quantity), updated.getStock(),
            "Stock adjusted successfully"));
        successCount++;
      } catch (IllegalArgumentException e) {
        try {
          Inventory inventory = findInventoryBySubSku(subSku);
          results.add(buildFailureResult(subSku, Math.abs(quantity), inventory.getStock(), e.getMessage()));
        } catch (Exception ex) {
          results.add(buildFailureResult(subSku, Math.abs(quantity), 0L, e.getMessage()));
        }
        failureCount++;
      } catch (ResourceNotFoundException e) {
        results.add(buildFailureResult(subSku, Math.abs(quantity), 0L,
            "Inventory not found for subSku: " + subSku));
        failureCount++;
      } catch (Exception e) {
        log.error("Error adjusting stock for subSku {}: {}", subSku, e.getMessage(), e);
        results.add(buildFailureResult(subSku, Math.abs(quantity), 0L,
            "Error adjusting stock: " + e.getMessage()));
        failureCount++;
      }
    }

    responseBuilder.addAllResults(results);
    responseBuilder.setSuccessCount(successCount);
    responseBuilder.setFailureCount(failureCount);
    responseBuilder.setAllSuccess(failureCount == 0);

    return responseBuilder.build();
  }

  // Helper methods for stock operations

  private long getTotalLockedQuantity(String subSku) {
    // This is a simplified implementation. In production, you'd want to:
    // 1. Use Redis SCAN to find all lock keys for this subSku
    // 2. Sum up all locked quantities
    // For now, we'll just return 0 as a placeholder
    // The actual implementation would depend on your Redis setup
    return 0; // TODO: Implement proper lock aggregation using Redis SCAN
  }

  private StockOperationResult buildSuccessResult(String subSku, long requestedQty, long currentStock, String message) {
    return StockOperationResult.newBuilder()
        .setSubSku(subSku)
        .setSuccess(true)
        .setMessage(message)
        .setCurrentStock(currentStock)
        .setRequestedQuantity(requestedQty)
        .build();
  }

  private StockOperationResult buildFailureResult(String subSku, long requestedQty, long currentStock, String message) {
    return StockOperationResult.newBuilder()
        .setSubSku(subSku)
        .setSuccess(false)
        .setMessage(message)
        .setCurrentStock(currentStock)
        .setRequestedQuantity(requestedQty)
        .build();
  }

  @Override
  protected String toId(String input) {
    return input;
  }

  @Override
  protected String getId(Inventory input) {
    return input.getId();
  }

  @Override
  protected String getIdFieldName() {
    return "_id";
  }
}




