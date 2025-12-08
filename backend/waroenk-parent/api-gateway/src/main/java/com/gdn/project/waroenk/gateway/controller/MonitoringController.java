package com.gdn.project.waroenk.gateway.controller;

import com.gdn.project.waroenk.gateway.dto.monitoring.*;
import com.gdn.project.waroenk.gateway.service.MonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * REST API for service monitoring dashboard.
 * Provides endpoints to fetch health, metrics, and info from all registered microservices.
 */
@RestController
@RequestMapping("/monitoring")
@RequiredArgsConstructor
@Tag(name = "Monitoring", description = "Service monitoring and metrics endpoints")
public class MonitoringController {

    private final MonitoringService monitoringService;

    @GetMapping("/summary")
    @Operation(summary = "Dashboard summary", description = "Get health status summary of all registered services")
    public Mono<ResponseEntity<DashboardSummaryDto>> getDashboardSummary() {
        return monitoringService.getDashboardSummary()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/services")
    @Operation(summary = "List all services", description = "Get list of all registered services with basic info")
    public ResponseEntity<List<ServiceHealthDto>> getAllServices() {
        return ResponseEntity.ok(monitoringService.getAllServices());
    }

    @GetMapping("/services/{serviceName}/health")
    @Operation(summary = "Service health", description = "Get detailed health status of a specific service")
    public Mono<ResponseEntity<ServiceHealthDto>> getServiceHealth(
            @Parameter(description = "Service name") @PathVariable String serviceName) {
        
        return monitoringService.getAllServices().stream()
                .filter(s -> s.getServiceName().equals(serviceName))
                .findFirst()
                .map(service -> monitoringService.checkServiceHealth(service)
                        .map(ResponseEntity::ok))
                .orElse(Mono.just(ResponseEntity.notFound().build()));
    }

    @GetMapping("/services/{serviceName}/metrics")
    @Operation(summary = "Service metrics", description = "Get detailed metrics (memory, CPU, threads, etc.) of a specific service")
    public Mono<ResponseEntity<ServiceMetricsDto>> getServiceMetrics(
            @Parameter(description = "Service name") @PathVariable String serviceName) {
        
        return monitoringService.getServiceMetrics(serviceName)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/services/{serviceName}/info")
    @Operation(summary = "Service info", description = "Get application info (version, build info) of a specific service")
    public Mono<ResponseEntity<ServiceInfoDto>> getServiceInfo(
            @Parameter(description = "Service name") @PathVariable String serviceName) {
        
        return monitoringService.getServiceInfo(serviceName)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/services/{serviceName}/metrics/history")
    @Operation(summary = "Service metrics history", description = "Get historical metrics data for time-series graphs")
    public ResponseEntity<List<ServiceMetricsDto>> getMetricsHistory(
            @Parameter(description = "Service name") @PathVariable String serviceName) {
        
        return ResponseEntity.ok(monitoringService.getMetricsHistory(serviceName));
    }

    @GetMapping("/services/{serviceName}/metrics/available")
    @Operation(summary = "Available metrics", description = "Get list of all available metric names for a service")
    public Mono<ResponseEntity<List<String>>> getAvailableMetrics(
            @Parameter(description = "Service name") @PathVariable String serviceName) {
        
        return monitoringService.getAvailableMetrics(serviceName)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/services/{serviceName}/metrics/{metricName}")
    @Operation(summary = "Specific metric", description = "Get a specific metric value by name")
    public Mono<ResponseEntity<Map<String, Object>>> getSpecificMetric(
            @Parameter(description = "Service name") @PathVariable String serviceName,
            @Parameter(description = "Metric name (e.g., jvm.memory.used)") @PathVariable String metricName) {
        
        return monitoringService.getSpecificMetric(serviceName, metricName)
                .map(ResponseEntity::ok);
    }
}





