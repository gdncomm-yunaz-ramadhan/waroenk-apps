package com.gdn.project.waroenk.catalog.mapper;

import com.gdn.project.waroenk.catalog.ContactInfo;
import com.gdn.project.waroenk.catalog.CreateMerchantRequest;
import com.gdn.project.waroenk.catalog.MerchantData;
import com.gdn.project.waroenk.catalog.entity.Merchant;
import com.google.protobuf.Timestamp;
import org.apache.commons.lang3.ObjectUtils;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.time.Instant;

@Mapper
public interface MerchantMapper extends GenericMapper {

  MerchantMapper INSTANCE = Mappers.getMapper(MerchantMapper.class);

  default MerchantData toResponseGrpc(Merchant entity) {
    MerchantData.Builder builder = MerchantData.newBuilder();
    builder.setId(entity.getId());
    builder.setName(entity.getName());
    builder.setCode(entity.getCode());

    if (entity.getIconUrl() != null) {
      builder.setIconUrl(entity.getIconUrl());
    }

    if (entity.getLocation() != null) {
      builder.setLocation(entity.getLocation());
    }

    if (entity.getContact() != null) {
      ContactInfo.Builder contactBuilder = ContactInfo.newBuilder();
      if (entity.getContact().getPhone() != null) {
        contactBuilder.setPhone(entity.getContact().getPhone());
      }
      if (entity.getContact().getEmail() != null) {
        contactBuilder.setEmail(entity.getContact().getEmail());
      }
      builder.setContact(contactBuilder.build());
    }

    if (entity.getRating() != null) {
      builder.setRating(entity.getRating());
    }

    if (entity.getCreatedAt() != null) {
      builder.setCreatedAt(toTimestamp(entity.getCreatedAt()));
    }
    if (entity.getUpdatedAt() != null) {
      builder.setUpdatedAt(toTimestamp(entity.getUpdatedAt()));
    }

    return builder.build();
  }

  default Merchant toEntity(CreateMerchantRequest request) {
    Merchant.MerchantBuilder builder = Merchant.builder();
    builder.name(request.getName());
    builder.code(request.getCode());
    builder.iconUrl(request.getIconUrl());
    builder.location(request.getLocation());

    if (ObjectUtils.isNotEmpty(request.getContact())) {
      Merchant.ContactInfo contact = Merchant.ContactInfo.builder()
          .phone(request.getContact().getPhone())
          .email(request.getContact().getEmail())
          .build();
      builder.contact(contact);
    }

    builder.rating(request.getRating());

    return builder.build();
  }
}
