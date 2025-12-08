package com.gdn.project.waroenk.gateway.dto;

import java.time.LocalDateTime;

public record ErrorResponseDto(
        int status,
        String message,
        String details,
        LocalDateTime timestamp
) {
}










