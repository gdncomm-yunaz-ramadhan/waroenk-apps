package com.gdn.project.waroenk.catalog.dto.systemparameter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpsertSystemParameterRequestDto(
    @NotBlank(message = "Variable is required") String variable,
    @NotNull(message = "Data is required") String data,
    String description
) {}










