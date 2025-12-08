package com.gdn.project.waroenk.gateway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GatewayResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public static <T> GatewayResponse<T> success(T data) {
        return GatewayResponse.<T>builder()
                .success(true)
                .message("OK")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> GatewayResponse<T> success(T data, String message) {
        return GatewayResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> GatewayResponse<T> error(String message) {
        return GatewayResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}











