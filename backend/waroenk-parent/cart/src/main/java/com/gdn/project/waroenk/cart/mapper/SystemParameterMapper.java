package com.gdn.project.waroenk.cart.mapper;

import com.gdn.project.waroenk.cart.SystemParameterData;
import com.gdn.project.waroenk.cart.UpsertSystemParameterRequest;
import com.gdn.project.waroenk.cart.entity.SystemParameter;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SystemParameterMapper extends GenericMapper {

  SystemParameterMapper INSTANCE = Mappers.getMapper(SystemParameterMapper.class);

  // Entity to gRPC
  default SystemParameterData toResponseGrpc(SystemParameter entity) {
    if (entity == null) {
      return null;
    }
    SystemParameterData.Builder builder = SystemParameterData.newBuilder();
    if (entity.getId() != null)
      builder.setId(entity.getId());
    if (entity.getVariable() != null)
      builder.setVariable(entity.getVariable());
    if (entity.getData() != null)
      builder.setData(entity.getData());
    if (entity.getDescription() != null)
      builder.setDescription(entity.getDescription());
    if (entity.getType() != null)
      builder.setType(entity.getType());
    if (entity.getCreatedAt() != null)
      builder.setCreatedAt(toTimestamp(entity.getCreatedAt()));
    if (entity.getUpdatedAt() != null)
      builder.setUpdatedAt(toTimestamp(entity.getUpdatedAt()));
    return builder.build();
  }

  // gRPC to Entity
  default SystemParameter toEntity(UpsertSystemParameterRequest request) {
    if (request == null) {
      return null;
    }
    return SystemParameter.builder()
        .variable(request.getVariable())
        .data(request.getData())
        .description(request.getDescription())
        .type(request.getType())
        .build();
  }
}



