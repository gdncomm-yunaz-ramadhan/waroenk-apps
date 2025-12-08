package com.gdn.project.waroenk.catalog.mapper;

import com.gdn.project.waroenk.catalog.CreateProductRequest;
import com.gdn.project.waroenk.catalog.ProductData;
import com.gdn.project.waroenk.catalog.ProductSummary;
import com.gdn.project.waroenk.catalog.entity.Product;
import org.apache.commons.lang3.ObjectUtils;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ProductMapper extends GenericMapper {

  ProductMapper INSTANCE = Mappers.getMapper(ProductMapper.class);

  default ProductData toResponseGrpc(Product entity) {
    ProductData.Builder builder = ProductData.newBuilder();
    builder.setId(entity.getId());
    builder.setTitle(entity.getTitle());
    builder.setSku(entity.getSku());
    builder.setMerchantCode(entity.getMerchantCode());
    builder.setCategoryId(entity.getCategoryId());
    builder.setBrandId(entity.getBrandId());

    if (entity.getSummary() != null) {
      ProductSummary.Builder summaryBuilder = ProductSummary.newBuilder();
      if (entity.getSummary().getShortDescription() != null) {
        summaryBuilder.setShortDescription(entity.getSummary().getShortDescription());
      }
      if (entity.getSummary().getTags() != null) {
        summaryBuilder.addAllTags(entity.getSummary().getTags());
      }
      builder.setSummary(summaryBuilder.build());
    }

    if (entity.getDetailRef() != null) {
      builder.setDetailRef(entity.getDetailRef());
    }
    if (entity.getCreatedAt() != null) {
      builder.setCreatedAt(toTimestamp(entity.getCreatedAt()));
    }
    if (entity.getUpdatedAt() != null) {
      builder.setUpdatedAt(toTimestamp(entity.getUpdatedAt()));
    }

    return builder.build();
  }

  default Product toEntity(CreateProductRequest request) {
    Product.ProductBuilder builder = Product.builder();
    builder.title(request.getTitle());
    builder.sku(request.getSku());
    builder.merchantCode(request.getMerchantCode());
    builder.categoryId(request.getCategoryId());
    builder.brandId(request.getBrandId());

    if (ObjectUtils.isNotEmpty(request.getSummary())) {
      Product.ProductSummary summary = Product.ProductSummary.builder()
          .shortDescription(request.getSummary().getShortDescription())
          .tags(request.getSummary().getTagsList())
          .build();
      builder.summary(summary);
    }

    builder.detailRef(request.getDetailRef());

    return builder.build();
  }
}
