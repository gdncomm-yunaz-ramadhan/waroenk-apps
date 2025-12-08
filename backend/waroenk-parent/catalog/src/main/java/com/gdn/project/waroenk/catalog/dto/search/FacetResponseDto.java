package com.gdn.project.waroenk.catalog.dto.search;

import java.util.List;

public record FacetResponseDto(
    List<FacetItemDto> categories,
    List<FacetItemDto> brands,
    List<FacetItemDto> merchants,
    Double minPrice,
    Double maxPrice
) {
  public record FacetItemDto(
      String id,
      String name,
      Integer count
  ) {}
}









