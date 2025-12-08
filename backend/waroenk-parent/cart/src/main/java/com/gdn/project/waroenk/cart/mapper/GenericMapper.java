package com.gdn.project.waroenk.cart.mapper;

import com.gdn.project.waroenk.cart.repository.MongoPageAble;
import com.gdn.project.waroenk.common.SortBy;
import com.google.protobuf.Timestamp;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.time.Instant;

@Mapper
public interface GenericMapper {

  GenericMapper INSTANCE = Mappers.getMapper(GenericMapper.class);

  default MongoPageAble.SortInfo toSortInfo(SortBy request) {
    MongoPageAble.SortInfo sort = ObjectUtils.isEmpty(request) ?
        MongoPageAble.SortInfo.defaultSort() :
        MongoPageAble.SortInfo.of(StringUtils.isBlank(request.getField()) ? "id" : request.getField(),
            StringUtils.isBlank(request.getDirection()) ? "asc" : request.getDirection());
    return sort;
  }

  default Timestamp toTimestamp(Instant instant) {
    if (instant == null) {
      return null;
    }
    return Timestamp.newBuilder().setSeconds(instant.getEpochSecond()).setNanos(instant.getNano()).build();
  }

  default Instant toInstant(Timestamp timestamp) {
    if (timestamp == null || (timestamp.getSeconds() == 0 && timestamp.getNanos() == 0)) {
      return null;
    }
    return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
  }
}


