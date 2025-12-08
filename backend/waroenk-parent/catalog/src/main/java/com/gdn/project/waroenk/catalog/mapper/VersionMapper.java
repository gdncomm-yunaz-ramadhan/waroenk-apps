package com.gdn.project.waroenk.catalog.mapper;

import com.gdn.project.waroenk.common.VersionResponse;
import com.gdn.project.waroenk.catalog.dto.VersionResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface VersionMapper {

  VersionMapper INSTANCE = Mappers.getMapper(VersionMapper.class);

}






