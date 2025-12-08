package com.gdn.project.waroenk.catalog.mapper;

import com.gdn.project.waroenk.catalog.CreateInventoryRequest;
import com.gdn.project.waroenk.catalog.InventoryData;
import com.gdn.project.waroenk.catalog.dto.inventory.CreateInventoryRequestDto;
import com.gdn.project.waroenk.catalog.dto.inventory.InventoryResponseDto;
import com.gdn.project.waroenk.catalog.entity.Inventory;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface InventoryMapper extends GenericMapper {

  InventoryMapper INSTANCE = Mappers.getMapper(InventoryMapper.class);

  InventoryResponseDto toResponseDto(InventoryData grpc);

  default InventoryData toResponseGrpc(Inventory entity) {
    InventoryData.Builder builder = InventoryData.newBuilder();
    builder.setId(entity.getId());
    builder.setSubSku(entity.getSubSku());
    builder.setStock(entity.getStock());
    if (entity.getCreatedAt() != null) {
      builder.setCreatedAt(toTimestamp(entity.getCreatedAt()));
    }
    if (entity.getUpdatedAt() != null) {
      builder.setUpdatedAt(toTimestamp(entity.getUpdatedAt()));
    }
    return builder.build();
  }

  default Inventory toEntity(CreateInventoryRequest request) {
    return Inventory.builder().subSku(request.getSubSku()).stock(request.getStock()).build();
  }

  default Inventory toEntity(CreateInventoryRequestDto dto) {
    return Inventory.builder().subSku(dto.subSku()).stock(dto.stock()).build();
  }
}






