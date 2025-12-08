package com.gdn.project.waroenk.catalog.dto.brand;

import jakarta.validation.constraints.NotBlank;

public record CreateBrandRequestDto(
    @NotBlank(message = "Name is required") String name,
    @NotBlank(message = "Icon Url is required") String iconUrl,
    @NotBlank(message = "Slug is required") String slug
) {}









