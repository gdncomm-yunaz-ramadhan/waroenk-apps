package com.gdn.project.waroenk.catalog.dto.systemparameter;

import java.time.Instant;

public record SystemParameterResponseDto(
    String id,
    String variable,
    String data,
    String description,
    Instant createdAt,
    Instant updatedAt
) {}









