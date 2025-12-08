package com.gdn.project.waroenk.catalog.mapper;

import com.gdn.project.waroenk.catalog.CreateVariantRequest;
import com.gdn.project.waroenk.catalog.VariantData;
import com.gdn.project.waroenk.catalog.VariantMedia;
import com.gdn.project.waroenk.catalog.entity.Variant;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.List;

@Mapper
public interface VariantMapper extends GenericMapper {

  VariantMapper INSTANCE = Mappers.getMapper(VariantMapper.class);

  default VariantData toResponseGrpc(Variant entity) {
    VariantData.Builder builder = VariantData.newBuilder();
    builder.setId(entity.getId());
    builder.setSku(entity.getSku());
    builder.setSubSku(entity.getSubSku());

    if (entity.getTitle() != null) {
      builder.setTitle(entity.getTitle());
    }

    builder.setPrice(entity.getPrice());
    builder.setIsDefault(entity.getIsDefault() != null && entity.getIsDefault());

    if (entity.getAttributes() != null) {
      builder.setAttributes(mapToStruct(entity.getAttributes()));
    }

    if (entity.getThumbnail() != null) {
      builder.setThumbnail(entity.getThumbnail());
    }

    if (entity.getMedia() != null) {
      for (Variant.VariantMedia media : entity.getMedia()) {
        VariantMedia.Builder mediaBuilder = VariantMedia.newBuilder()
            .setUrl(media.getUrl())
            .setType(media.getType())
            .setSortOrder(media.getSortOrder() != null ? media.getSortOrder() : 0);
        if (media.getAltText() != null) {
          mediaBuilder.setAltText(media.getAltText());
        }
        builder.addMedia(mediaBuilder.build());
      }
    }

    if (entity.getCreatedAt() != null) {
      builder.setCreatedAt(toTimestamp(entity.getCreatedAt()));
    }
    if (entity.getUpdatedAt() != null) {
      builder.setUpdatedAt(toTimestamp(entity.getUpdatedAt()));
    }

    return builder.build();
  }

  default Variant toEntity(CreateVariantRequest request) {
    List<Variant.VariantMedia> mediaList = new ArrayList<>();
    for (VariantMedia media : request.getMediaList()) {
      mediaList.add(Variant.VariantMedia.builder()
          .url(media.getUrl())
          .type(media.getType())
          .sortOrder(media.getSortOrder())
          .altText(media.getAltText())
          .build());
    }

    return Variant.builder()
        .sku(request.getSku())
        .title(request.getTitle())
        .price(request.getPrice())
        .isDefault(request.getIsDefault())
        .attributes(structToMap(request.getAttributes()))
        .thumbnail(request.getThumbnail())
        .media(mediaList)
        .build();
  }
}
