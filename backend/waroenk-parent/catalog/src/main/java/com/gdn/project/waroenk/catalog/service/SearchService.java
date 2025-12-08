package com.gdn.project.waroenk.catalog.service;

import com.gdn.project.waroenk.catalog.dto.inventory.InventoryCheckItemDto;
import com.gdn.project.waroenk.catalog.dto.merchant.MerchantResponseDto;
import com.gdn.project.waroenk.catalog.dto.product.AggregatedProductDto;
import com.gdn.project.waroenk.catalog.dto.product.ProductDetailDto;
import com.gdn.project.waroenk.catalog.entity.Merchant;
import org.typesense.model.FacetCounts;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface SearchService {

  /**
   * Search for products with query parameters map.
   * Supports: q (query text), category, brand, merchantCode, merchantLocation
   * Category filter uses categoryCodes for hierarchy matching.
   */
  Result<AggregatedProductDto> searchProducts(Map<String, String> queries, int size, String cursor, String sortBy, String sortOrder, Boolean buyable)
      throws Exception;

  /**
   * Search for merchants by query
   */
  Result<MerchantResponseDto> searchMerchants(String query, int size, String cursor, String sortBy, String sortOrder)
      throws Exception;

  /**
   * Combined search across products and merchants
   */
  CompletableFuture<CombinedResult> search(String query, int size, String cursor, String sortBy, String sortOrder);

  /**
   * Index a product (called on create/update)
   */
  void indexProduct(AggregatedProductDto product);

  /**
   * Remove a product from the index (called on delete)
   */
  void deleteProductIndex(String productId);

  /**
   * Index a merchant (called on create/update)
   */
  void indexMerchant(Merchant merchant);

  /**
   * Remove a merchant from the index (called on delete)
   */
  void deleteMerchantIndex(String merchantId);

  /**
   * Build and get AggregatedProductDto from product SKU
   */
  List<AggregatedProductDto> buildAggregatedProduct(String sku);

  /**
   * Index all merchants in TypeSense (bulk operation)
   * @return number of merchants indexed
   */
  int indexAllMerchants();

  /**
   * Index all products in TypeSense (bulk operation)
   * @return number of products indexed
   */
  int indexAllProducts();

  /**
   * Index products by SKUs (selective/incremental indexing)
   * @param skus list of product SKUs to index
   * @return result with indexed count and failed SKUs
   */
  BulkIndexResult indexProductsBySkus(List<String> skus);

  /**
   * Get verbose product details by ID (subSku or sku).
   * Returns complete product info with merchant, brand, category, inventory, and variant images.
   * Uses caching: merchant/brand/category (1 hour TTL), inventory (30 seconds TTL).
   */
  ProductDetailsResult getProductDetails(String id) throws Exception;

  /**
   * Get product summary by multiple subSkus.
   * Queries Typesense by exact subSku match.
   */
  ProductSummaryResult getProductSummary(List<String> subSkus) throws Exception;

  /**
   * Check inventory for multiple subSkus.
   * Returns stock info with hasStock boolean.
   * Uses short TTL caching (30 seconds).
   */
  InventoryCheckResult checkInventory(List<String> subSkus);

  record CombinedResult(Result<AggregatedProductDto> products, Result<MerchantResponseDto> merchants, int totalReturned,
                        int totalMatch, long took, String nextToken) {
  }

  record Result<T>(List<T> contents, List<FacetCounts> facetCounts, int totalReturned, int totalMatch, int totalPage, long took, String nextToken) {
  }

  record ProductDetailsResult(ProductDetailDto product, long took) {
  }

  record ProductSummaryResult(List<AggregatedProductDto> products, int totalFound, int totalRequested, long took) {
  }

  record InventoryCheckResult(List<InventoryCheckItemDto> items, int totalFound, int totalRequested, long took) {
  }

  record BulkIndexResult(int totalRequested, int totalIndexed, int totalFailed, List<String> failedSkus) {
  }
}



