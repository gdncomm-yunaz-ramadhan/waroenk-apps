package com.gdn.project.waroenk.gateway.service;

import com.gdn.project.waroenk.gateway.config.GatewayProperties;
import com.gdn.project.waroenk.gateway.config.GrpcChannelConfig;
import com.gdn.project.waroenk.gateway.dto.monitoring.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for monitoring gRPC microservices via their actuator endpoints.
 * Fetches health, metrics, and info from each registered service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService {

    private final GatewayProperties gatewayProperties;
    private final GrpcChannelConfig channelConfig;
    private final DynamicRoutingRegistry dynamicRegistry;
    
    private final WebClient.Builder webClientBuilder;
    
    // Cache for metrics history (service -> list of metrics snapshots)
    private final Map<String, List<ServiceMetricsDto>> metricsHistory = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY_SIZE = 60; // Keep last 60 snapshots (e.g., 1 hour at 1 min intervals)
    
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    /**
     * Get all registered services (static + dynamic)
     */
    public List<ServiceHealthDto> getAllServices() {
        List<ServiceHealthDto> services = new ArrayList<>();
        
        // Add static services from properties
        gatewayProperties.getServices().forEach((name, config) -> {
            services.add(ServiceHealthDto.builder()
                    .serviceName(name)
                    .host(config.getHost())
                    .grpcPort(config.getPort())
                    .httpPort(config.getHttpPort())
                    .source("static")
                    .status("UNKNOWN")
                    .build());
        });
        
        // Add dynamic services from registry
        for (DynamicRoutingRegistry.CachedService cachedService : dynamicRegistry.getAllServices()) {
            // Skip if already in static list
            if (services.stream().noneMatch(s -> s.getServiceName().equals(cachedService.name()))) {
                services.add(ServiceHealthDto.builder()
                        .serviceName(cachedService.name())
                        .host(cachedService.host())
                        .grpcPort(cachedService.port())
                        .httpPort(0) // Dynamic services may not have HTTP port registered
                        .source("dynamic")
                        .status("UNKNOWN")
                        .build());
            }
        }
        
        return services;
    }

    /**
     * Get dashboard summary with health status of all services
     */
    public Mono<DashboardSummaryDto> getDashboardSummary() {
        List<ServiceHealthDto> services = getAllServices();
        
        return Flux.fromIterable(services)
                .flatMap(this::checkServiceHealth)
                .collectList()
                .map(healthResults -> {
                    int healthy = (int) healthResults.stream().filter(s -> "UP".equals(s.getStatus())).count();
                    int unhealthy = (int) healthResults.stream().filter(s -> "DOWN".equals(s.getStatus())).count();
                    int unknown = (int) healthResults.stream().filter(s -> "UNKNOWN".equals(s.getStatus())).count();
                    
                    return DashboardSummaryDto.builder()
                            .timestamp(LocalDateTime.now())
                            .totalServices(healthResults.size())
                            .healthyServices(healthy)
                            .unhealthyServices(unhealthy)
                            .unknownServices(unknown)
                            .services(healthResults)
                            .build();
                });
    }

    /**
     * Check health status of a single service
     */
    public Mono<ServiceHealthDto> checkServiceHealth(ServiceHealthDto service) {
        if (service.getHttpPort() <= 0) {
            // No HTTP port configured, mark as unknown
            return Mono.just(service.toBuilder()
                    .status("UNKNOWN")
                    .lastCheck(LocalDateTime.now())
                    .errorMessage("HTTP port not configured for monitoring")
                    .build());
        }
        
        String baseUrl = buildBaseUrl(service.getHost(), service.getHttpPort());
        WebClient client = webClientBuilder.baseUrl(baseUrl).build();
        
        return client.get()
                .uri("/actuator/health")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(TIMEOUT)
                .map(health -> {
                    String status = (String) health.getOrDefault("status", "UNKNOWN");
                    return service.toBuilder()
                            .status(status)
                            .lastCheck(LocalDateTime.now())
                            .healthDetails(health)
                            .errorMessage(null)
                            .build();
                })
                .onErrorResume(e -> {
                    log.warn("Failed to check health for service {}: {}", service.getServiceName(), e.getMessage());
                    return Mono.just(service.toBuilder()
                            .status("DOWN")
                            .lastCheck(LocalDateTime.now())
                            .errorMessage(getErrorMessage(e))
                            .build());
                });
    }

    /**
     * Get detailed metrics for a specific service
     */
    public Mono<ServiceMetricsDto> getServiceMetrics(String serviceName) {
        ServiceHealthDto service = getAllServices().stream()
                .filter(s -> s.getServiceName().equals(serviceName))
                .findFirst()
                .orElse(null);
        
        if (service == null) {
            return Mono.just(ServiceMetricsDto.builder()
                    .serviceName(serviceName)
                    .timestamp(LocalDateTime.now())
                    .available(false)
                    .errorMessage("Service not found: " + serviceName)
                    .build());
        }
        
        if (service.getHttpPort() <= 0) {
            return Mono.just(ServiceMetricsDto.builder()
                    .serviceName(serviceName)
                    .timestamp(LocalDateTime.now())
                    .available(false)
                    .errorMessage("HTTP port not configured for service: " + serviceName)
                    .build());
        }
        
        String baseUrl = buildBaseUrl(service.getHost(), service.getHttpPort());
        WebClient client = webClientBuilder.baseUrl(baseUrl).build();
        
        // Fetch multiple metrics in parallel
        Mono<Map<String, Object>> heapUsed = fetchMetric(client, "jvm.memory.used", Map.of("area", "heap"));
        Mono<Map<String, Object>> heapMax = fetchMetric(client, "jvm.memory.max", Map.of("area", "heap"));
        Mono<Map<String, Object>> heapCommitted = fetchMetric(client, "jvm.memory.committed", Map.of("area", "heap"));
        Mono<Map<String, Object>> nonHeapUsed = fetchMetric(client, "jvm.memory.used", Map.of("area", "nonheap"));
        Mono<Map<String, Object>> threads = fetchMetric(client, "jvm.threads.live", Map.of());
        Mono<Map<String, Object>> threadsPeak = fetchMetric(client, "jvm.threads.peak", Map.of());
        Mono<Map<String, Object>> threadsDaemon = fetchMetric(client, "jvm.threads.daemon", Map.of());
        Mono<Map<String, Object>> cpuUsage = fetchMetric(client, "process.cpu.usage", Map.of());
        Mono<Map<String, Object>> systemCpu = fetchMetric(client, "system.cpu.usage", Map.of());
        Mono<Map<String, Object>> uptime = fetchMetric(client, "process.uptime", Map.of());
        Mono<Map<String, Object>> gcPause = fetchMetric(client, "jvm.gc.pause", Map.of());
        Mono<Map<String, Object>> classesLoaded = fetchMetric(client, "jvm.classes.loaded", Map.of());
        Mono<Map<String, Object>> httpRequests = fetchMetric(client, "http.server.requests", Map.of());
        Mono<Map<String, Object>> diskFree = fetchMetric(client, "disk.free", Map.of());
        Mono<Map<String, Object>> diskTotal = fetchMetric(client, "disk.total", Map.of());
        
        return Mono.zip(heapUsed, heapMax, heapCommitted, nonHeapUsed, threads, threadsPeak, threadsDaemon, cpuUsage)
                .zipWith(Mono.zip(systemCpu, uptime, gcPause, classesLoaded, httpRequests, diskFree, diskTotal))
                .map(tuple -> {
                    var first = tuple.getT1();
                    var second = tuple.getT2();
                    
                    Long heapUsedVal = extractMetricValue(first.getT1());
                    Long heapMaxVal = extractMetricValue(first.getT2());
                    Long heapCommittedVal = extractMetricValue(first.getT3());
                    Long nonHeapUsedVal = extractMetricValue(first.getT4());
                    
                    Double heapPercentage = (heapUsedVal != null && heapMaxVal != null && heapMaxVal > 0) 
                            ? (heapUsedVal.doubleValue() / heapMaxVal.doubleValue()) * 100 
                            : null;
                    
                    ServiceMetricsDto.MemoryMetrics memory = ServiceMetricsDto.MemoryMetrics.builder()
                            .heapUsed(heapUsedVal)
                            .heapMax(heapMaxVal)
                            .heapCommitted(heapCommittedVal)
                            .nonHeapUsed(nonHeapUsedVal)
                            .heapUsedPercentage(heapPercentage)
                            .build();
                    
                    ServiceMetricsDto.JvmMetrics jvm = ServiceMetricsDto.JvmMetrics.builder()
                            .threadCount(extractMetricValueAsInt(first.getT5()))
                            .threadPeakCount(extractMetricValueAsInt(first.getT6()))
                            .threadDaemonCount(extractMetricValueAsInt(first.getT7()))
                            .cpuUsage(extractMetricValueAsDouble(first.getT8()))
                            .classesLoaded(extractMetricValue(second.getT4()))
                            .uptimeSeconds(extractMetricValue(second.getT2()))
                            .gcPauseCount(extractMetricCount(second.getT3()))
                            .gcPauseTime(extractMetricTotal(second.getT3()))
                            .build();
                    
                    ServiceMetricsDto.SystemMetrics system = ServiceMetricsDto.SystemMetrics.builder()
                            .cpuUsage(extractMetricValueAsDouble(second.getT1()))
                            .diskFreeSpace(extractMetricValue(second.getT6()))
                            .diskTotalSpace(extractMetricValue(second.getT7()))
                            .build();
                    
                    ServiceMetricsDto.HttpMetrics http = ServiceMetricsDto.HttpMetrics.builder()
                            .totalRequests(extractMetricCount(second.getT5()))
                            .avgResponseTime(extractMetricMean(second.getT5()))
                            .build();
                    
                    ServiceMetricsDto metrics = ServiceMetricsDto.builder()
                            .serviceName(serviceName)
                            .timestamp(LocalDateTime.now())
                            .available(true)
                            .memory(memory)
                            .jvm(jvm)
                            .system(system)
                            .http(http)
                            .build();
                    
                    // Store in history
                    storeMetricsHistory(serviceName, metrics);
                    
                    return metrics;
                })
                .onErrorResume(e -> {
                    log.warn("Failed to fetch metrics for service {}: {}", serviceName, e.getMessage());
                    return Mono.just(ServiceMetricsDto.builder()
                            .serviceName(serviceName)
                            .timestamp(LocalDateTime.now())
                            .available(false)
                            .errorMessage(getErrorMessage(e))
                            .build());
                });
    }

    /**
     * Get service info (version, build info, etc.)
     */
    public Mono<ServiceInfoDto> getServiceInfo(String serviceName) {
        ServiceHealthDto service = getAllServices().stream()
                .filter(s -> s.getServiceName().equals(serviceName))
                .findFirst()
                .orElse(null);
        
        if (service == null || service.getHttpPort() <= 0) {
            return Mono.just(ServiceInfoDto.builder()
                    .serviceName(serviceName)
                    .available(false)
                    .errorMessage(service == null ? "Service not found" : "HTTP port not configured")
                    .build());
        }
        
        String baseUrl = buildBaseUrl(service.getHost(), service.getHttpPort());
        WebClient client = webClientBuilder.baseUrl(baseUrl).build();
        
        return client.get()
                .uri("/actuator/info")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(TIMEOUT)
                .map(info -> {
                    Map<String, Object> app = getNestedMap(info, "app");
                    Map<String, Object> build = getNestedMap(info, "build");
                    Map<String, Object> git = getNestedMap(info, "git");
                    Map<String, Object> java = getNestedMap(info, "java");
                    
                    return ServiceInfoDto.builder()
                            .serviceName(serviceName)
                            .appName((String) app.getOrDefault("name", build.getOrDefault("artifact", serviceName)))
                            .version((String) app.getOrDefault("version", build.getOrDefault("version", "unknown")))
                            .javaVersion(java != null ? (String) java.get("version") : null)
                            .gitInfo(git)
                            .additionalInfo(info)
                            .available(true)
                            .build();
                })
                .onErrorResume(e -> {
                    log.warn("Failed to fetch info for service {}: {}", serviceName, e.getMessage());
                    return Mono.just(ServiceInfoDto.builder()
                            .serviceName(serviceName)
                            .available(false)
                            .errorMessage(getErrorMessage(e))
                            .build());
                });
    }

    /**
     * Get metrics history for a service (for time-series graphs)
     */
    public List<ServiceMetricsDto> getMetricsHistory(String serviceName) {
        return metricsHistory.getOrDefault(serviceName, Collections.emptyList());
    }

    /**
     * Get all available metric names for a service
     */
    public Mono<List<String>> getAvailableMetrics(String serviceName) {
        ServiceHealthDto service = getAllServices().stream()
                .filter(s -> s.getServiceName().equals(serviceName))
                .findFirst()
                .orElse(null);
        
        if (service == null || service.getHttpPort() <= 0) {
            return Mono.just(Collections.emptyList());
        }
        
        String baseUrl = buildBaseUrl(service.getHost(), service.getHttpPort());
        WebClient client = webClientBuilder.baseUrl(baseUrl).build();
        
        return client.get()
                .uri("/actuator/metrics")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(TIMEOUT)
                .map(response -> {
                    Object names = response.get("names");
                    if (names instanceof List) {
                        return ((List<?>) names).stream()
                                .filter(String.class::isInstance)
                                .map(String.class::cast)
                                .sorted()
                                .toList();
                    }
                    return Collections.<String>emptyList();
                })
                .onErrorResume(e -> Mono.just(Collections.emptyList()));
    }

    /**
     * Get specific metric value for a service
     */
    public Mono<Map<String, Object>> getSpecificMetric(String serviceName, String metricName) {
        ServiceHealthDto service = getAllServices().stream()
                .filter(s -> s.getServiceName().equals(serviceName))
                .findFirst()
                .orElse(null);
        
        if (service == null || service.getHttpPort() <= 0) {
            return Mono.just(Map.of("error", "Service not found or HTTP port not configured"));
        }
        
        String baseUrl = buildBaseUrl(service.getHost(), service.getHttpPort());
        WebClient client = webClientBuilder.baseUrl(baseUrl).build();
        
        return fetchMetric(client, metricName, Map.of())
                .onErrorResume(e -> Mono.just(Map.of("error", getErrorMessage(e))));
    }

    // Helper methods
    
    private String buildBaseUrl(String host, int port) {
        return String.format("http://%s:%d", host, port);
    }
    
    private Mono<Map<String, Object>> fetchMetric(WebClient client, String metricName, Map<String, String> tags) {
        StringBuilder uri = new StringBuilder("/actuator/metrics/" + metricName);
        if (!tags.isEmpty()) {
            uri.append("?");
            tags.forEach((k, v) -> uri.append("tag=").append(k).append(":").append(v).append("&"));
        }
        
        return client.get()
                .uri(uri.toString())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(TIMEOUT)
                .onErrorResume(e -> Mono.just(Collections.emptyMap()));
    }
    
    private Long extractMetricValue(Map<String, Object> metric) {
        if (metric == null || metric.isEmpty()) return null;
        Object measurements = metric.get("measurements");
        if (measurements instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map<?, ?> map) {
                Object value = map.get("value");
                if (value instanceof Number num) {
                    return num.longValue();
                }
            }
        }
        return null;
    }
    
    private Integer extractMetricValueAsInt(Map<String, Object> metric) {
        Long value = extractMetricValue(metric);
        return value != null ? value.intValue() : null;
    }
    
    private Double extractMetricValueAsDouble(Map<String, Object> metric) {
        if (metric == null || metric.isEmpty()) return null;
        Object measurements = metric.get("measurements");
        if (measurements instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map<?, ?> map) {
                Object value = map.get("value");
                if (value instanceof Number num) {
                    return num.doubleValue();
                }
            }
        }
        return null;
    }
    
    private Long extractMetricCount(Map<String, Object> metric) {
        if (metric == null || metric.isEmpty()) return null;
        Object measurements = metric.get("measurements");
        if (measurements instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    if ("COUNT".equals(map.get("statistic"))) {
                        Object value = map.get("value");
                        if (value instanceof Number num) {
                            return num.longValue();
                        }
                    }
                }
            }
        }
        return null;
    }
    
    private Double extractMetricTotal(Map<String, Object> metric) {
        if (metric == null || metric.isEmpty()) return null;
        Object measurements = metric.get("measurements");
        if (measurements instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    if ("TOTAL_TIME".equals(map.get("statistic"))) {
                        Object value = map.get("value");
                        if (value instanceof Number num) {
                            return num.doubleValue();
                        }
                    }
                }
            }
        }
        return null;
    }
    
    private Double extractMetricMean(Map<String, Object> metric) {
        if (metric == null || metric.isEmpty()) return null;
        Object measurements = metric.get("measurements");
        if (measurements instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    if ("MEAN".equals(map.get("statistic"))) {
                        Object value = map.get("value");
                        if (value instanceof Number num) {
                            return num.doubleValue();
                        }
                    }
                }
            }
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> getNestedMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return Collections.emptyMap();
    }
    
    private void storeMetricsHistory(String serviceName, ServiceMetricsDto metrics) {
        metricsHistory.computeIfAbsent(serviceName, k -> Collections.synchronizedList(new ArrayList<>()));
        List<ServiceMetricsDto> history = metricsHistory.get(serviceName);
        history.add(metrics);
        
        // Keep only last MAX_HISTORY_SIZE entries
        while (history.size() > MAX_HISTORY_SIZE) {
            history.remove(0);
        }
    }
    
    private String getErrorMessage(Throwable e) {
        if (e instanceof WebClientResponseException wcre) {
            return String.format("HTTP %d: %s", wcre.getStatusCode().value(), wcre.getStatusText());
        }
        if (e.getCause() != null) {
            return e.getCause().getMessage();
        }
        return e.getMessage();
    }
}




