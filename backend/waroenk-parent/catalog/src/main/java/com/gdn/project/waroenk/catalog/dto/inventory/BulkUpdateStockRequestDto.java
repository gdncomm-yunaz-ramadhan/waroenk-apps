package com.gdn.project.waroenk.catalog.dto.inventory;

import java.util.List;

public record BulkUpdateStockRequestDto(
    List<StockUpdateItem> items
) {
  public record StockUpdateItem(String subSku, Long stock) {}
}









