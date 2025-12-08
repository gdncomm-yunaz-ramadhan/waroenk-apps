package com.gdn.project.waroenk.catalog.mapper;

import com.gdn.project.waroenk.common.Basic;
import com.gdn.project.waroenk.common.SortBy;
import com.gdn.project.waroenk.catalog.dto.BasicDto;
import com.gdn.project.waroenk.catalog.dto.SortByDto;
import com.google.protobuf.Struct;
import com.google.protobuf.Timestamp;
import com.google.protobuf.Value;
import org.apache.commons.lang3.ObjectUtils;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Mapper
public interface GenericMapper {

  GenericMapper INSTANCE = Mappers.getMapper(GenericMapper.class);

  default SortBy toSortByGrpc(SortByDto sortByDto) {
    if (sortByDto == null) {
      return null;
    }
    return SortBy.newBuilder()
        .setField(sortByDto.field() != null ? sortByDto.field() : "id")
        .setDirection(sortByDto.direction() != null ? sortByDto.direction() : "asc")
        .build();
  }

  SortByDto toSortByDto(SortBy sortByGrpc);

  BasicDto toBasicDto(Basic basic);

  default Timestamp toTimestamp(Instant instant) {
    return Timestamp.newBuilder().setSeconds(instant.getEpochSecond()).setNanos(instant.getNano()).build();
  }

  default Instant toInstant(Timestamp timestamp) {
    return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
  }

  default Struct mapToStruct(Map<String, Object> map) {
    Struct.Builder structBuilder = Struct.newBuilder();
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      structBuilder.putFields(entry.getKey(), objectToValue(entry.getValue()));
    }
    return structBuilder.build();
  }

  default Value objectToValue(Object obj) {
    if (obj == null) {
      return Value.newBuilder().setNullValue(com.google.protobuf.NullValue.NULL_VALUE).build();
    } else if (obj instanceof String string) {
      return Value.newBuilder().setStringValue(string).build();
    } else if (obj instanceof Number number) {
      return Value.newBuilder().setNumberValue(number.doubleValue()).build();
    } else if (obj instanceof Boolean bool) {
      return Value.newBuilder().setBoolValue(bool).build();
    } else {
      return Value.newBuilder().setStringValue(obj.toString()).build();
    }
  }

  default Map<String, Object> structToMap(Struct struct) {
    Map<String, Object> map = new HashMap<>();
    if (ObjectUtils.isEmpty(struct)) {
      return map;
    }
    for (Map.Entry<String, Value> entry : struct.getFieldsMap().entrySet()) {
      map.put(entry.getKey(), valueToObject(entry.getValue()));
    }
    return map;
  }

  default Object valueToObject(Value value) {
    return switch (value.getKindCase()) {
      case STRING_VALUE -> value.getStringValue();
      case NUMBER_VALUE -> value.getNumberValue();
      case BOOL_VALUE -> value.getBoolValue();
      case NULL_VALUE -> null;
      default -> value.getStringValue();
    };
  }
}






