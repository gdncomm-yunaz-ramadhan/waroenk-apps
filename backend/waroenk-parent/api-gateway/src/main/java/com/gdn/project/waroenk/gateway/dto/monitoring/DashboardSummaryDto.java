package com.gdn.project.waroenk.gateway.dto.monitoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDto {
    private LocalDateTime timestamp;
    private int totalServices;
    private int healthyServices;
    private int unhealthyServices;
    private int unknownServices;
    private List<ServiceHealthDto> services;
}




