package com.gdn.project.waroenk.catalog.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Verbose product details DTO with all related entity information.
 * Used for the /product/details endpoint.
 */
public record ProductDetailDto(
    String id,
    String sku,
    String title,
    String shortDescription,
    List<String> tags,
    String detailRef,
    MerchantDetailDto merchant,
    BrandDetailDto brand,
    CategoryDetailDto category,
    List<VariantDetailDto> variants,
    long totalStock,
    boolean hasStock,
    Instant createdAt,
    Instant updatedAt
) {
  
  public record MerchantDetailDto(
      String id,
      String name,
      String code,
      String iconUrl,
      String location,
      Float rating,
      ContactInfoDto contact
  ) {
    public record ContactInfoDto(String phone, String email) {}
  }
  
  public record BrandDetailDto(
      String id,
      String name,
      String slug,
      String iconUrl
  ) {}
  
  public record CategoryDetailDto(
      String id,
      String name,
      String slug,
      String iconUrl,
      String parentId
  ) {}
  
  public record VariantDetailDto(
      String id,
      String subSku,
      String title,
      Double price,
      Boolean isDefault,
      Map<String, Object> attributes,
      String thumbnail,
      List<VariantMediaDto> media,
      StockInfoDto stockInfo,
      Instant createdAt,
      Instant updatedAt
  ) {
    public record VariantMediaDto(
        String url,
        String type,
        Integer sortOrder,
        String altText
    ) {}
    
    public record StockInfoDto(
        long totalStock,
        boolean hasStock,
        Instant updatedAt
    ) {}
  }
}
