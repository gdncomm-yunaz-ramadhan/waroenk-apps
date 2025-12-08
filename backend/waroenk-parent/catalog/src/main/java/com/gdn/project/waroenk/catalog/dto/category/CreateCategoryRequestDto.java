package com.gdn.project.waroenk.catalog.dto.category;

import jakarta.validation.constraints.NotBlank;

public record CreateCategoryRequestDto(
    @NotBlank(message = "Name is required") String name,
    @NotBlank(message = "Slug is required") String slug,
    @NotBlank(message = "Icon Url is required") String iconUrl,
    String parentId
) {}









