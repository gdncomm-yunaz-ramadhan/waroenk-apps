package com.gdn.project.waroenk.catalog.mapper;

import com.gdn.project.waroenk.catalog.BrandData;
import com.gdn.project.waroenk.catalog.CreateBrandRequest;
import com.gdn.project.waroenk.catalog.dto.brand.CreateBrandRequestDto;
import com.gdn.project.waroenk.catalog.entity.Brand;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface BrandMapper extends GenericMapper {

  BrandMapper INSTANCE = Mappers.getMapper(BrandMapper.class);

  default BrandData toResponseGrpc(Brand entity) {
    BrandData.Builder builder = BrandData.newBuilder();
    builder.setId(entity.getId());
    builder.setName(entity.getName());
    builder.setSlug(entity.getSlug());
    if (entity.getIconUrl() != null) {
      builder.setIconUrl(entity.getIconUrl());
    }
    if (entity.getCreatedAt() != null) {
      builder.setCreatedAt(toTimestamp(entity.getCreatedAt()));
    }
    if (entity.getUpdatedAt() != null) {
      builder.setUpdatedAt(toTimestamp(entity.getUpdatedAt()));
    }
    return builder.build();
  }

  default Brand toEntity(CreateBrandRequest request) {
    return Brand.builder().name(request.getName()).slug(request.getSlug()).iconUrl(request.getIconUrl()).build();
  }

  default Brand toEntity(CreateBrandRequestDto dto) {
    return Brand.builder().name(dto.name()).slug(dto.slug()).iconUrl(dto.iconUrl()).build();
  }
}



