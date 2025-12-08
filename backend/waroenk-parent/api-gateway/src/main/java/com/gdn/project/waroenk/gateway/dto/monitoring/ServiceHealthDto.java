package com.gdn.project.waroenk.gateway.dto.monitoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ServiceHealthDto {
    private String serviceName;
    private String host;
    private int grpcPort;
    private int httpPort;
    private String status;  // UP, DOWN, UNKNOWN
    private String source;  // static, dynamic
    private LocalDateTime lastCheck;
    private Map<String, Object> healthDetails;
    private String errorMessage;
}

