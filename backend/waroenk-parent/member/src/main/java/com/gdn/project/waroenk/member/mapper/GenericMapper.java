package com.gdn.project.waroenk.member.mapper;

import com.gdn.project.waroenk.common.SortBy;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface GenericMapper {

  GenericMapper INSTANCE = Mappers.getMapper(GenericMapper.class);

  default SortBy toSortByGrpc(String field, String direction) {
    return SortBy.newBuilder().setField(field).setDirection(direction).build();
  }

}
