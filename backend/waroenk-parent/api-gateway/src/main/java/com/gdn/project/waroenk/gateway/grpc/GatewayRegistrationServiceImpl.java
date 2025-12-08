package com.gdn.project.waroenk.gateway.grpc;

import com.gdn.project.waroenk.gateway.*;
import com.gdn.project.waroenk.gateway.service.DynamicRoutingRegistry;
import com.gdn.project.waroenk.gateway.service.DynamicRoutingRegistry.RouteDefinitionDto;
import com.gdn.project.waroenk.gateway.service.DynamicRoutingRegistry.ServiceDefinition;
import com.gdn.project.waroenk.gateway.service.DynamicRoutingRegistry.RegistrationResult;
import com.gdn.project.waroenk.gateway.service.DynamicRoutingRegistry.RouteHashCheck;
import com.gdn.project.waroenk.gateway.service.DynamicRoutingRegistry.RouteCheckResult;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * gRPC service implementation for microservice registration.
 * Microservices call this service to register their routes with the gateway.
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class GatewayRegistrationServiceImpl extends GatewayRegistrationServiceGrpc.GatewayRegistrationServiceImplBase {

    private static final int DEFAULT_HEARTBEAT_INTERVAL = 30; // seconds

    private final DynamicRoutingRegistry routingRegistry;

    @Override
    public void registerService(com.gdn.project.waroenk.gateway.ServiceDefinition request,
                                StreamObserver<RegistrationAck> responseObserver) {
        log.info("Received registration request from service: {} at {}:{}",
                request.getName(), request.getHost(), request.getPort());

        try {
            // Convert proto to internal DTO
            ServiceDefinition definition = toServiceDefinition(request);

            // Register the service
            RegistrationResult result = routingRegistry.registerService(definition);

            // Build response
            RegistrationAck response = RegistrationAck.newBuilder()
                    .setSuccess(result.success())
                    .setMessage(result.message())
                    .setRoutesRegistered(result.routesRegistered())
                    .setRoutesSkipped(result.routesSkipped())
                    .setServiceId(result.serviceId())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("Service {} registered successfully: {} routes added, {} skipped",
                    request.getName(), result.routesRegistered(), result.routesSkipped());

        } catch (Exception e) {
            log.error("Failed to register service {}: {}", request.getName(), e.getMessage(), e);

            RegistrationAck errorResponse = RegistrationAck.newBuilder()
                    .setSuccess(false)
                    .setMessage("Registration failed: " + e.getMessage())
                    .setRoutesRegistered(0)
                    .setRoutesSkipped(0)
                    .build();

            responseObserver.onNext(errorResponse);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void unregisterService(UnregisterRequest request,
                                  StreamObserver<RegistrationAck> responseObserver) {
        log.info("Received unregister request for service: {}", request.getServiceName());

        try {
            boolean success = routingRegistry.unregisterService(request.getServiceName());

            RegistrationAck response = RegistrationAck.newBuilder()
                    .setSuccess(success)
                    .setMessage(success ? "Service unregistered successfully" : "Service not found")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Failed to unregister service {}: {}", request.getServiceName(), e.getMessage(), e);

            RegistrationAck errorResponse = RegistrationAck.newBuilder()
                    .setSuccess(false)
                    .setMessage("Unregistration failed: " + e.getMessage())
                    .build();

            responseObserver.onNext(errorResponse);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void heartbeat(ServicePing request, StreamObserver<HeartbeatAck> responseObserver) {
        log.debug("Heartbeat from service: {}", request.getServiceName());

        try {
            boolean active = routingRegistry.updateHeartbeat(request.getServiceName());

            Instant now = Instant.now();
            HeartbeatAck response = HeartbeatAck.newBuilder()
                    .setActive(active)
                    .setHeartbeatInterval(DEFAULT_HEARTBEAT_INTERVAL)
                    .setServerTime(Timestamp.newBuilder()
                            .setSeconds(now.getEpochSecond())
                            .setNanos(now.getNano())
                            .build())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Failed to process heartbeat for {}: {}", request.getServiceName(), e.getMessage(), e);

            HeartbeatAck errorResponse = HeartbeatAck.newBuilder()
                    .setActive(false)
                    .setHeartbeatInterval(DEFAULT_HEARTBEAT_INTERVAL)
                    .build();

            responseObserver.onNext(errorResponse);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void checkRoutes(RouteCheckRequest request, StreamObserver<RouteCheckResponse> responseObserver) {
        log.debug("Check routes request from service: {}", request.getServiceName());

        try {
            // Convert proto to internal DTOs
            List<RouteHashCheck> routeChecks = request.getRoutesList().stream()
                    .map(r -> new RouteHashCheck(r.getHttpMethod(), r.getPath(), r.getRouteHash()))
                    .collect(Collectors.toList());

            RouteCheckResult result = routingRegistry.checkRoutes(request.getServiceName(), routeChecks);

            // Build response
            RouteCheckResponse.Builder responseBuilder = RouteCheckResponse.newBuilder();

            for (RouteHashCheck toRegister : result.routesToRegister()) {
                responseBuilder.addRoutesToRegister(
                        com.gdn.project.waroenk.gateway.RouteHashCheck.newBuilder()
                                .setHttpMethod(toRegister.httpMethod())
                                .setPath(toRegister.path())
                                .setRouteHash(toRegister.routeHash())
                                .build()
                );
            }

            for (RouteHashCheck upToDate : result.routesUpToDate()) {
                responseBuilder.addRoutesUpToDate(
                        com.gdn.project.waroenk.gateway.RouteHashCheck.newBuilder()
                                .setHttpMethod(upToDate.httpMethod())
                                .setPath(upToDate.path())
                                .setRouteHash(upToDate.routeHash())
                                .build()
                );
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Failed to check routes for {}: {}", request.getServiceName(), e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to check routes: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    // ==================== Helper Methods ====================

    private ServiceDefinition toServiceDefinition(com.gdn.project.waroenk.gateway.ServiceDefinition proto) {
        List<RouteDefinitionDto> routes = proto.getRoutesList().stream()
                .map(this::toRouteDefinitionDto)
                .collect(Collectors.toList());

        return new ServiceDefinition(
                proto.getName(),
                proto.getProtocol().isEmpty() ? "grpc" : proto.getProtocol(),
                proto.getHost(),
                proto.getPort(),
                proto.getUseTls(),
                proto.getDescriptorUrl().isEmpty() ? null : proto.getDescriptorUrl(),
                proto.getVersion().isEmpty() ? null : proto.getVersion(),
                routes
        );
    }

    private RouteDefinitionDto toRouteDefinitionDto(RouteDefinition proto) {
        return new RouteDefinitionDto(
                proto.getHttpMethod(),
                proto.getPath(),
                proto.getGrpcService(),
                proto.getGrpcMethod(),
                proto.getRequestType().isEmpty() ? null : proto.getRequestType(),
                proto.getResponseType().isEmpty() ? null : proto.getResponseType(),
                proto.getPublicEndpoint(),
                proto.getRequiredRolesList()
        );
    }
}











