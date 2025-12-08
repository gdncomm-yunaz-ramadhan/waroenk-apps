package com.gdn.project.waroenk.gateway.service;

import com.gdn.project.waroenk.gateway.config.GrpcChannelConfig;
import io.grpc.Channel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Registry for gRPC service channels.
 * 
 * NOTE: This class has been significantly simplified!
 * 
 * Previously, this class maintained a hardcoded registry of all proto message types
 * and provided methods to parse JSON to protobuf messages. This approach had several
 * drawbacks:
 * - Required manual registration of every new message type
 * - Tight coupling between gateway and service proto definitions
 * - Maintenance overhead when adding new services/messages
 * 
 * The new approach uses gRPC Server Reflection (via ReflectionGrpcClient) which:
 * - Dynamically discovers service/method descriptors at runtime
 * - Uses DynamicMessage for JSON<->protobuf conversion
 * - Makes the gateway truly service-agnostic
 * 
 * This class now only provides channel management functionality.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrpcServiceRegistry {

    private final GrpcChannelConfig channelConfig;

    @PostConstruct
    public void init() {
        log.info("GrpcServiceRegistry initialized (using reflection-based dynamic discovery)");
    }

    /**
     * Get the channel for a service
     */
    public Channel getChannel(String serviceName) {
        return channelConfig.getChannel(serviceName);
    }
}
