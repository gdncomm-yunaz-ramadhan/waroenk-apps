package com.gdn.project.waroenk.catalog.model;

import java.time.Instant;

public record MerchantResponseDto(
    String id,
    String name,
    String code,
    String iconUrl,
    String location,
    ContactInfoDto contact,
    Float rating,
    Instant createdAt,
    Instant updatedAt
) {
  public record ContactInfoDto(String phone, String email) {}
}
