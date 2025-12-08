package com.gdn.project.waroenk.member.mapper;

import com.gdn.project.waroenk.member.AddressData;
import com.gdn.project.waroenk.member.UpsertAddressRequest;
import com.gdn.project.waroenk.member.entity.Address;
import com.google.protobuf.Timestamp;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Mapper
public interface AddressMapper extends GenericMapper {

  AddressMapper INSTANCE = Mappers.getMapper(AddressMapper.class);

  default AddressData toAddressData(Address address) {
    return toAddressData(address, null);
  }

  default AddressData toAddressData(Address address, java.util.UUID defaultAddressId) {
    if (address == null) {
      return null;
    }

    AddressData.Builder builder = AddressData.newBuilder();

    if (address.getId() != null) {
      builder.setId(address.getId().toString());
      if (defaultAddressId != null && address.getId().equals(defaultAddressId)) {
        builder.setIsDefault(true);
      }
    }
    if (address.getLabel() != null) {
      builder.setLabel(address.getLabel());
    }
    if (address.getCountry() != null) {
      builder.setCountry(address.getCountry());
    }
    if (address.getProvince() != null) {
      builder.setProvince(address.getProvince());
    }
    if (address.getCity() != null) {
      builder.setCity(address.getCity());
    }
    if (address.getDistrict() != null) {
      builder.setDistrict(address.getDistrict());
    }
    if (address.getSubdistrict() != null) {
      builder.setSubDistrict(address.getSubdistrict());
    }
    if (address.getPostalCode() != null) {
      builder.setPostalCode(address.getPostalCode());
    }
    if (address.getStreet() != null) {
      builder.setStreet(address.getStreet());
    }
    if (address.getLatitude() != null) {
      builder.setLatitude(address.getLatitude().floatValue());
    }
    if (address.getLongitude() != null) {
      builder.setLongitude(address.getLongitude().floatValue());
    }
    if (address.getDetails() != null) {
      builder.setDetails(address.getDetails());
    }
    if (address.getCreatedAt() != null) {
      builder.setCreatedAt(localDateTimeToTimestamp(address.getCreatedAt()));
    }
    if (address.getUpdatedAt() != null) {
      builder.setUpdatedAt(localDateTimeToTimestamp(address.getUpdatedAt()));
    }

    return builder.build();
  }

  default Address toAddressEntity(UpsertAddressRequest grpc) {
    if (grpc == null) {
      return null;
    }
    return Address.builder()
        .label(grpc.getLabel())
        .country(grpc.getCountry())
        .province(grpc.getProvince())
        .city(grpc.getCity())
        .district(grpc.getDistrict())
        .subdistrict(grpc.getSubDistrict())
        .postalCode(grpc.getPostalCode())
        .street(grpc.getStreet())
        .latitude(grpc.getLatitude() != 0 ? BigDecimal.valueOf(grpc.getLatitude()) : null)
        .longitude(grpc.getLongitude() != 0 ? BigDecimal.valueOf(grpc.getLongitude()) : null)
        .details(grpc.getDetails())
        .build();
  }

  default Timestamp localDateTimeToTimestamp(LocalDateTime localDateTime) {
    if (localDateTime == null) {
      return null;
    }
    Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    return Timestamp.newBuilder()
        .setSeconds(instant.getEpochSecond())
        .setNanos(instant.getNano())
        .build();
  }

  default LocalDateTime toLocalDateTime(Timestamp value) {
    if (value == null || (value.getSeconds() == 0 && value.getNanos() == 0)) {
      return null;
    }
    return LocalDateTime.ofInstant(
        Instant.ofEpochSecond(value.getSeconds(), value.getNanos()),
        ZoneId.systemDefault()
    );
  }
}
