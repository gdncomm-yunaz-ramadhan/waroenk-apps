package com.gdn.project.waroenk.catalog.service;

import com.gdn.project.waroenk.catalog.FilterVariantRequest;
import com.gdn.project.waroenk.catalog.FindVariantsBySkuRequest;
import com.gdn.project.waroenk.catalog.MultipleVariantResponse;
import com.gdn.project.waroenk.catalog.entity.Variant;

public interface VariantService {
  Variant createVariant(Variant variant);
  Variant updateVariant(String id, Variant variant);
  Variant findVariantById(String id);
  Variant findVariantBySubSku(String subSku);
  MultipleVariantResponse findVariantsBySku(FindVariantsBySkuRequest request);
  boolean deleteVariant(String id);
  MultipleVariantResponse filterVariants(FilterVariantRequest request);
  
  /**
   * Set a variant as the default variant for its product.
   * This will:
   * 1. Unset isDefault on all other variants with the same SKU
   * 2. Set isDefault=true on the specified variant
   * 3. Update the parent product's title and summary from this variant
   */
  Variant setDefaultVariant(String variantId);
  
  /**
   * Find the default variant for a product SKU
   */
  Variant findDefaultVariantBySku(String sku);
}
