package com.gdn.project.waroenk.gateway.dto.monitoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceInfoDto {
    private String serviceName;
    private String appName;
    private String version;
    private String javaVersion;
    private String springBootVersion;
    private LocalDateTime buildTime;
    private Map<String, Object> gitInfo;
    private Map<String, Object> additionalInfo;
    private boolean available;
    private String errorMessage;
}





