package com.gdn.project.waroenk.cart.mapper;

import com.gdn.project.waroenk.cart.AddressSnapshotData;
import com.gdn.project.waroenk.cart.CheckoutData;
import com.gdn.project.waroenk.cart.CheckoutItemData;
import com.gdn.project.waroenk.cart.dto.checkout.CheckoutItemDto;
import com.gdn.project.waroenk.cart.dto.checkout.CheckoutResponseDto;
import com.gdn.project.waroenk.cart.entity.AddressSnapshot;
import com.gdn.project.waroenk.cart.entity.Checkout;
import com.gdn.project.waroenk.cart.entity.CheckoutItem;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Mapper
public interface CheckoutMapper extends GenericMapper {

  CheckoutMapper INSTANCE = Mappers.getMapper(CheckoutMapper.class);

  // Entity to gRPC
  default CheckoutData toResponseGrpc(Checkout entity) {
    if (entity == null) {
      return null;
    }
    CheckoutData.Builder builder = CheckoutData.newBuilder();
    if (entity.getCheckoutId() != null)
      builder.setCheckoutId(entity.getCheckoutId());
    if (entity.getUserId() != null)
      builder.setUserId(entity.getUserId());
    if (entity.getOrderId() != null)
      builder.setOrderId(entity.getOrderId());
    if (entity.getPaymentCode() != null)
      builder.setPaymentCode(entity.getPaymentCode());
    if (entity.getSourceCartId() != null)
      builder.setSourceCartId(entity.getSourceCartId());
    if (entity.getTotalPrice() != null)
      builder.setTotalPrice(entity.getTotalPrice());
    if (entity.getCurrency() != null)
      builder.setCurrency(entity.getCurrency());
    builder.setStatus(entity.getEffectiveStatus()); // Use effective status
    if (entity.getShippingAddress() != null) {
      builder.setShippingAddress(toAddressSnapshotGrpc(entity.getShippingAddress()));
    }
    if (entity.getLockedAt() != null)
      builder.setLockedAt(toTimestamp(entity.getLockedAt()));
    if (entity.getExpiresAt() != null)
      builder.setExpiresAt(toTimestamp(entity.getExpiresAt()));
    if (entity.getCreatedAt() != null)
      builder.setCreatedAt(toTimestamp(entity.getCreatedAt()));
    if (entity.getPaidAt() != null)
      builder.setPaidAt(toTimestamp(entity.getPaidAt()));
    if (entity.getCancelledAt() != null)
      builder.setCancelledAt(toTimestamp(entity.getCancelledAt()));

    if (entity.getItems() != null) {
      entity.getItems().forEach(item -> builder.addItems(toCheckoutItemDataGrpc(item)));
    }

    return builder.build();
  }

  default CheckoutItemData toCheckoutItemDataGrpc(CheckoutItem item) {
    if (item == null) {
      return null;
    }
    CheckoutItemData.Builder builder = CheckoutItemData.newBuilder();
    if (item.getSku() != null)
      builder.setSku(item.getSku());
    if (item.getSubSku() != null)
      builder.setSubSku(item.getSubSku());
    if (item.getTitle() != null)
      builder.setTitle(item.getTitle());
    if (item.getPriceSnapshot() != null)
      builder.setPriceSnapshot(item.getPriceSnapshot());
    if (item.getQuantity() != null)
      builder.setQuantity(item.getQuantity());
    if (item.getAvailableStockSnapshot() != null)
      builder.setAvailableStockSnapshot(item.getAvailableStockSnapshot());
    if (item.getImageUrl() != null)
      builder.setImageUrl(item.getImageUrl());
    if (item.getAttributes() != null)
      builder.putAllAttributes(item.getAttributes());
    if (item.getReserved() != null)
      builder.setReserved(item.getReserved());
    if (item.getReservationError() != null)
      builder.setReservationError(item.getReservationError());
    return builder.build();
  }

  default AddressSnapshotData toAddressSnapshotGrpc(AddressSnapshot address) {
    if (address == null) {
      return null;
    }
    AddressSnapshotData.Builder builder = AddressSnapshotData.newBuilder();
    if (address.getRecipientName() != null)
      builder.setRecipientName(address.getRecipientName());
    if (address.getPhone() != null)
      builder.setPhone(address.getPhone());
    if (address.getStreet() != null)
      builder.setStreet(address.getStreet());
    if (address.getCity() != null)
      builder.setCity(address.getCity());
    if (address.getProvince() != null)
      builder.setProvince(address.getProvince());
    if (address.getDistrict() != null)
      builder.setDistrict(address.getDistrict());
    if (address.getSubDistrict() != null)
      builder.setSubDistrict(address.getSubDistrict());
    if (address.getCountry() != null)
      builder.setCountry(address.getCountry());
    if (address.getPostalCode() != null)
      builder.setPostalCode(address.getPostalCode());
    if (address.getNotes() != null)
      builder.setNotes(address.getNotes());
    if (address.getLatitude() != null)
      builder.setLatitude(address.getLatitude());
    if (address.getLongitude() != null)
      builder.setLongitude(address.getLongitude());
    return builder.build();
  }

  default AddressSnapshot toAddressSnapshotEntity(AddressSnapshotData grpc) {
    if (grpc == null) {
      return null;
    }
    return AddressSnapshot.builder()
        .recipientName(grpc.getRecipientName())
        .phone(grpc.getPhone())
        .street(grpc.getStreet())
        .city(grpc.getCity())
        .province(grpc.getProvince())
        .district(grpc.getDistrict())
        .subDistrict(grpc.getSubDistrict())
        .country(grpc.getCountry())
        .postalCode(grpc.getPostalCode())
        .notes(grpc.getNotes())
        .latitude(grpc.getLatitude())
        .longitude(grpc.getLongitude())
        .build();
  }

  // gRPC to DTO
  default CheckoutResponseDto toResponseDto(CheckoutData grpc) {
    if (grpc == null) {
      return null;
    }
    List<CheckoutItemDto> items =
        grpc.getItemsList().stream().map(this::toCheckoutItemDto).collect(Collectors.toList());

    return new CheckoutResponseDto(grpc.getCheckoutId(),
        grpc.getUserId(),
        grpc.getOrderId(),
        grpc.getPaymentCode(),
        grpc.getSourceCartId(),
        items,
        grpc.getTotalPrice(),
        grpc.getCurrency(),
        grpc.getStatus(),
        toAddressSnapshotDto(grpc.hasShippingAddress() ? grpc.getShippingAddress() : null),
        toInstant(grpc.getLockedAt()),
        toInstant(grpc.getExpiresAt()),
        toInstant(grpc.getCreatedAt()),
        grpc.hasPaidAt() ? toInstant(grpc.getPaidAt()) : null,
        grpc.hasCancelledAt() ? toInstant(grpc.getCancelledAt()) : null);
  }

  default CheckoutResponseDto.AddressSnapshotDto toAddressSnapshotDto(AddressSnapshotData grpc) {
    if (grpc == null) {
      return null;
    }
    return new CheckoutResponseDto.AddressSnapshotDto(grpc.getRecipientName(),
        grpc.getPhone(),
        grpc.getStreet(),
        grpc.getCity(),
        grpc.getProvince(),
        grpc.getDistrict(),
        grpc.getSubDistrict(),
        grpc.getCountry(),
        grpc.getPostalCode(),
        grpc.getNotes(),
        grpc.getLatitude(),
        grpc.getLongitude());
  }

  default CheckoutItemDto toCheckoutItemDto(CheckoutItemData grpc) {
    if (grpc == null) {
      return null;
    }
    return new CheckoutItemDto(grpc.getSku(),
        grpc.getSubSku(),
        grpc.getTitle(),
        grpc.getPriceSnapshot(),
        grpc.getQuantity(),
        grpc.getAvailableStockSnapshot(),
        grpc.getImageUrl(),
        new HashMap<>(grpc.getAttributesMap()),
        grpc.getReserved(),
        grpc.getReservationError());
  }
}





