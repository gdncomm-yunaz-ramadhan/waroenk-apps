package com.gdn.project.waroenk.gateway.dto.monitoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceMetricsDto {
    private String serviceName;
    private LocalDateTime timestamp;
    private boolean available;
    private String errorMessage;
    
    // Memory metrics
    private MemoryMetrics memory;
    
    // JVM metrics
    private JvmMetrics jvm;
    
    // HTTP metrics
    private HttpMetrics http;
    
    // System metrics
    private SystemMetrics system;
    
    // Raw metrics (for custom display)
    private Map<String, Object> rawMetrics;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemoryMetrics {
        private Long heapUsed;
        private Long heapMax;
        private Long heapCommitted;
        private Long nonHeapUsed;
        private Long nonHeapCommitted;
        private Double heapUsedPercentage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JvmMetrics {
        private Integer threadCount;
        private Integer threadPeakCount;
        private Integer threadDaemonCount;
        private Long classesLoaded;
        private Long gcPauseCount;
        private Double gcPauseTime;
        private Double cpuUsage;
        private Long uptimeSeconds;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HttpMetrics {
        private Long totalRequests;
        private Double requestsPerSecond;
        private Double avgResponseTime;
        private Long errorCount;
        private Map<String, Long> requestsByStatus;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemMetrics {
        private Double cpuUsage;
        private Integer cpuCount;
        private Long diskFreeSpace;
        private Long diskTotalSpace;
    }
}




