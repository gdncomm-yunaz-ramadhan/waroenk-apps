package com.gdn.project.waroenk.catalog.dto;

import java.time.LocalDateTime;

public record ErrorResponseDto(
    Integer code,
    String status,
    String message,
    LocalDateTime timestamp
) {}










