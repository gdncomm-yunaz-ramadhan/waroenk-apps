package com.gdn.project.waroenk.catalog.mapper;

import com.gdn.project.waroenk.catalog.SystemParameterData;
import com.gdn.project.waroenk.catalog.UpsertSystemParameterRequest;
import com.gdn.project.waroenk.catalog.entity.SystemParameter;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SystemParameterMapper extends GenericMapper {

  SystemParameterMapper INSTANCE = Mappers.getMapper(SystemParameterMapper.class);

  default SystemParameterData toResponseGrpc(SystemParameter entity) {
    SystemParameterData.Builder builder = SystemParameterData.newBuilder();
    builder.setId(entity.getId());
    builder.setVariable(entity.getVariable());
    if (entity.getData() != null) {
      builder.setData(entity.getData());
    }
    if (entity.getDescription() != null) {
      builder.setDescription(entity.getDescription());
    }
    if (entity.getCreatedAt() != null) {
      builder.setCreatedAt(toTimestamp(entity.getCreatedAt()));
    }
    if (entity.getUpdatedAt() != null) {
      builder.setUpdatedAt(toTimestamp(entity.getUpdatedAt()));
    }
    return builder.build();
  }

  default SystemParameter toEntity(UpsertSystemParameterRequest request) {
    return SystemParameter.builder()
        .variable(request.getVariable())
        .data(request.getData())
        .description(request.getDescription())
        .build();
  }
}






