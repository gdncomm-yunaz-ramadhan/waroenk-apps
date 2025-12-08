package com.gdn.project.waroenk.catalog.service;

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
import com.gdn.project.waroenk.catalog.entity.Inventory;

import java.util.List;

public interface InventoryService {
  Inventory createInventory(Inventory inventory);
  Inventory updateInventory(String id, Inventory inventory);
  Inventory updateStockBySubSku(String subSku, Long stock);
  Inventory adjustStock(String subSku, Long quantity);
  BulkUpdateStockResponse bulkUpdateStock(BulkUpdateStockRequest request);
  Inventory findInventoryById(String id);
  Inventory findInventoryBySubSku(String subSku);
  boolean deleteInventory(String id);
  List<Inventory> findBulkInventoriesBySubSkus(List<String> subSkus);
  MultipleInventoryResponse filterInventory(FilterInventoryRequest request);

  /**
   * Bulk lock stock for checkout - reserve inventory.
   * Creates a temporary lock with TTL. Stock is not actually reduced until acquired.
   * Uses Redis for lock management.
   */
  BulkLockStockResponse bulkLockStock(BulkLockStockRequest request);

  /**
   * Bulk acquire stock - confirm reservation and reduce stock.
   * Should be called after successful payment to finalize the stock reduction.
   */
  BulkAcquireStockResponse bulkAcquireStock(BulkAcquireStockRequest request);

  /**
   * Bulk release stock - return reserved stock.
   * Should be called when checkout is cancelled or expired.
   */
  BulkReleaseStockResponse bulkReleaseStock(BulkReleaseStockRequest request);

  /**
   * Bulk adjust stock - adjust stock by quantity (positive or negative).
   */
  BulkAdjustStockResponse bulkAdjustStock(BulkAdjustStockRequest request);
}




