package com.gdn.project.waroenk.cart.mapper;

import com.gdn.project.waroenk.cart.CartData;
import com.gdn.project.waroenk.cart.CartItemData;
import com.gdn.project.waroenk.cart.entity.Cart;
import com.gdn.project.waroenk.cart.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface CartMapper extends GenericMapper {

  CartMapper INSTANCE = Mappers.getMapper(CartMapper.class);

  // Entity to gRPC
  default CartData toResponseGrpc(Cart entity) {
    if (entity == null) {
      return null;
    }
    CartData.Builder builder = CartData.newBuilder();
    if (entity.getId() != null)
      builder.setId(entity.getId());
    if (entity.getUserId() != null)
      builder.setUserId(entity.getUserId());
    if (entity.getCurrency() != null)
      builder.setCurrency(entity.getCurrency());
    builder.setTotalAmount(entity.getTotalAmount());
    builder.setTotalItems(entity.getTotalItems());
    if (entity.getVersion() != null)
      builder.setVersion(entity.getVersion());
    if (entity.getCreatedAt() != null)
      builder.setCreatedAt(toTimestamp(entity.getCreatedAt()));
    if (entity.getUpdatedAt() != null)
      builder.setUpdatedAt(toTimestamp(entity.getUpdatedAt()));

    if (entity.getItems() != null) {
      entity.getItems().forEach(item -> builder.addItems(toCartItemDataGrpc(item)));
    }

    return builder.build();
  }

  default CartItemData toCartItemDataGrpc(CartItem item) {
    if (item == null) {
      return null;
    }
    CartItemData.Builder builder = CartItemData.newBuilder();
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
    return builder.build();
  }
}




