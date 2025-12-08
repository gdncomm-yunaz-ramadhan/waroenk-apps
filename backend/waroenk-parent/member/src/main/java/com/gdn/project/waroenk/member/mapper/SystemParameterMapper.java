package com.gdn.project.waroenk.member.mapper;

import com.gdn.project.waroenk.member.SystemParameterData;
import com.gdn.project.waroenk.member.UpsertSystemParameterRequest;
import com.gdn.project.waroenk.member.entity.SystemParameter;
import com.google.protobuf.Timestamp;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Mapper
public interface SystemParameterMapper extends GenericMapper {

  SystemParameterMapper INSTANCE = Mappers.getMapper(SystemParameterMapper.class);

  default SystemParameterData toResponseGrpc(SystemParameter entity) {
    if (entity == null) {
      return null;
    }
    SystemParameterData.Builder builder = SystemParameterData.newBuilder();
    if (entity.getId() != null) {
      builder.setId(entity.getId().toString());
    }
    if (entity.getData() != null) {
      builder.setData(entity.getData());
    }
    if (entity.getVariable() != null) {
      builder.setVariable(entity.getVariable());
    }
    if (entity.getDescription() != null) {
      builder.setDescription(entity.getDescription());
    }

    if (entity.getCreatedAt() != null) {
      Instant createdInstant = entity.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant();
      builder.setCreatedAt(Timestamp.newBuilder()
          .setSeconds(createdInstant.getEpochSecond())
          .setNanos(createdInstant.getNano())
          .build());
    }

    if (entity.getUpdatedAt() != null) {
      Instant updatedInstant = entity.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant();
      builder.setUpdatedAt(Timestamp.newBuilder()
          .setSeconds(updatedInstant.getEpochSecond())
          .setNanos(updatedInstant.getNano())
          .build());
    }

    return builder.build();
  }

  SystemParameter toSystemParameterEntity(SystemParameterData grpc);

  SystemParameter toSystemParameterEntity(UpsertSystemParameterRequest grpc);

  default LocalDateTime toLocalDateTime(Timestamp value) {
    if (value == null || (value.getSeconds() == 0 && value.getNanos() == 0)) {
      return null;
    }
    return LocalDateTime.ofInstant(Instant.ofEpochSecond(value.getSeconds(), value.getNanos()), ZoneId.systemDefault());
  }
}
