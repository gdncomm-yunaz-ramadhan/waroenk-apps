package com.gdn.project.waroenk.gateway.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.MethodDescriptor;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.reflection.v1alpha.ServerReflectionGrpc;
import io.grpc.reflection.v1alpha.ServerReflectionRequest;
import io.grpc.reflection.v1alpha.ServerReflectionResponse;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/**
 * Reflection-based gRPC client that dynamically discovers service descriptors.
 * This enables the gateway to invoke any gRPC method without compile-time knowledge
 * of the proto types - making the gateway truly service-agnostic.
 * 
 * Key features:
 * - Uses gRPC Server Reflection to fetch service/method descriptors at runtime
 * - Caches descriptors for performance (with TTL)
 * - Uses DynamicMessage for JSON to protobuf conversion without generated classes
 */
@Slf4j
@Component
public class ReflectionGrpcClient {

    // Cache for file descriptors: "serviceName:host:port" -> FileDescriptor
    private final Cache<String, Descriptors.FileDescriptor> fileDescriptorCache = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(Duration.ofMinutes(30))
            .recordStats()
            .build();

    // Cache for method descriptors: "fullMethodName:host:port" -> MethodInfo
    private final Cache<String, MethodInfo> methodCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterAccess(Duration.ofHours(1))
            .recordStats()
            .build();

    // JSON parser/printer for protobuf
    private final JsonFormat.Parser jsonParser = JsonFormat.parser().ignoringUnknownFields();
    private final JsonFormat.Printer jsonPrinter = JsonFormat.printer()
            .preservingProtoFieldNames()
            .includingDefaultValueFields();

    /**
     * Invoke a gRPC method dynamically using reflection.
     * This is the main entry point for the gateway proxy.
     *
     * @param channel         The gRPC channel to the target service
     * @param grpcServiceName Full gRPC service name (e.g., "member.user.UserService")
     * @param grpcMethodName  Method name (e.g., "Register")
     * @param jsonRequest     Request body as JSON
     * @return Response as JSON string
     */
    public String invokeMethod(Channel channel, String grpcServiceName, String grpcMethodName, String jsonRequest) 
            throws Exception {
        
        String channelKey = getChannelKey(channel);
        String methodKey = grpcServiceName + "/" + grpcMethodName + ":" + channelKey;

        // Get or fetch method info (with caching)
        MethodInfo methodInfo = methodCache.get(methodKey, key -> {
            try {
                return fetchMethodInfo(channel, grpcServiceName, grpcMethodName);
            } catch (Exception e) {
                log.error("Failed to fetch method info for {}: {}", key, e.getMessage());
                throw new RuntimeException("Failed to discover gRPC method: " + e.getMessage(), e);
            }
        });

        if (methodInfo == null) {
            throw new IllegalStateException("Could not discover method: " + grpcServiceName + "/" + grpcMethodName);
        }

        // Parse JSON to DynamicMessage
        DynamicMessage.Builder requestBuilder = DynamicMessage.newBuilder(methodInfo.inputType());
        if (jsonRequest != null && !jsonRequest.isBlank()) {
            jsonParser.merge(jsonRequest, requestBuilder);
        }
        DynamicMessage request = requestBuilder.build();

        // Build method descriptor for the call
        MethodDescriptor<DynamicMessage, DynamicMessage> methodDescriptor = MethodDescriptor
                .<DynamicMessage, DynamicMessage>newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(grpcServiceName + "/" + grpcMethodName)
                .setRequestMarshaller(ProtoUtils.marshaller(
                        DynamicMessage.getDefaultInstance(methodInfo.inputType())))
                .setResponseMarshaller(ProtoUtils.marshaller(
                        DynamicMessage.getDefaultInstance(methodInfo.outputType())))
                .build();

        // Make the blocking unary call
        DynamicMessage response = ClientCalls.blockingUnaryCall(
                channel,
                methodDescriptor,
                CallOptions.DEFAULT,
                request
        );

        // Convert response to JSON
        return jsonPrinter.print(response);
    }

    /**
     * Fetch method info using gRPC Server Reflection.
     */
    private MethodInfo fetchMethodInfo(Channel channel, String grpcServiceName, String grpcMethodName) 
            throws Exception {
        
        log.debug("Fetching method info via reflection: {}/{}", grpcServiceName, grpcMethodName);

        // Get file descriptor for the service
        Descriptors.FileDescriptor fileDescriptor = fetchFileDescriptor(channel, grpcServiceName);
        
        // Find the service
        Descriptors.ServiceDescriptor serviceDescriptor = findService(fileDescriptor, grpcServiceName);
        if (serviceDescriptor == null) {
            throw new IllegalArgumentException("Service not found: " + grpcServiceName);
        }

        // Find the method
        Descriptors.MethodDescriptor methodDescriptor = serviceDescriptor.findMethodByName(grpcMethodName);
        if (methodDescriptor == null) {
            throw new IllegalArgumentException("Method not found: " + grpcMethodName + " in service " + grpcServiceName);
        }

        return new MethodInfo(
                methodDescriptor.getInputType(),
                methodDescriptor.getOutputType(),
                methodDescriptor
        );
    }

    /**
     * Fetch file descriptor using gRPC Server Reflection.
     */
    private Descriptors.FileDescriptor fetchFileDescriptor(Channel channel, String grpcServiceName) 
            throws Exception {
        
        String channelKey = getChannelKey(channel);
        String cacheKey = grpcServiceName + ":" + channelKey;

        Descriptors.FileDescriptor cached = fileDescriptorCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }

        log.debug("Fetching file descriptor for service: {}", grpcServiceName);

        // Use reflection to get file descriptor
        CompletableFuture<DescriptorProtos.FileDescriptorProto> future = new CompletableFuture<>();
        Map<String, DescriptorProtos.FileDescriptorProto> dependencyMap = new ConcurrentHashMap<>();

        ServerReflectionGrpc.ServerReflectionStub reflectionStub = 
                ServerReflectionGrpc.newStub(channel);

        StreamObserver<ServerReflectionRequest> requestObserver = 
                reflectionStub.serverReflectionInfo(new StreamObserver<ServerReflectionResponse>() {
                    
                    private final Queue<String> pendingDependencies = new ConcurrentLinkedQueue<>();
                    private StreamObserver<ServerReflectionRequest> outbound;
                    private boolean mainFileReceived = false;
                    private DescriptorProtos.FileDescriptorProto mainFile = null;

                    @Override
                    public void onNext(ServerReflectionResponse response) {
                        if (response.hasFileDescriptorResponse()) {
                            for (var bytes : response.getFileDescriptorResponse().getFileDescriptorProtoList()) {
                                try {
                                    DescriptorProtos.FileDescriptorProto fdp = 
                                            DescriptorProtos.FileDescriptorProto.parseFrom(bytes);
                                    
                                    dependencyMap.put(fdp.getName(), fdp);
                                    
                                    // Queue dependencies
                                    for (String dep : fdp.getDependencyList()) {
                                        if (!dependencyMap.containsKey(dep)) {
                                            pendingDependencies.add(dep);
                                        }
                                    }
                                    
                                    if (!mainFileReceived) {
                                        mainFileReceived = true;
                                        mainFile = fdp;
                                    }
                                } catch (Exception e) {
                                    log.error("Failed to parse file descriptor: {}", e.getMessage());
                                }
                            }
                        }

                        // Process pending dependencies
                        String nextDep = pendingDependencies.poll();
                        if (nextDep != null && outbound != null) {
                            outbound.onNext(ServerReflectionRequest.newBuilder()
                                    .setFileByFilename(nextDep)
                                    .build());
                        } else if (mainFile != null) {
                            // All dependencies loaded
                            future.complete(mainFile);
                            if (outbound != null) {
                                outbound.onCompleted();
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        log.error("Reflection error: {}", t.getMessage());
                        future.completeExceptionally(t);
                    }

                    @Override
                    public void onCompleted() {
                        if (!future.isDone() && mainFile != null) {
                            future.complete(mainFile);
                        } else if (!future.isDone()) {
                            future.completeExceptionally(
                                    new RuntimeException("Reflection completed without finding service"));
                        }
                    }

                    public void setOutbound(StreamObserver<ServerReflectionRequest> outbound) {
                        this.outbound = outbound;
                    }
                });

        // Set the outbound reference for dependency fetching
        if (requestObserver instanceof StreamObserver) {
            // Request the service file
            requestObserver.onNext(ServerReflectionRequest.newBuilder()
                    .setFileContainingSymbol(grpcServiceName)
                    .build());
        }

        // Wait for result with timeout
        DescriptorProtos.FileDescriptorProto mainFile;
        try {
            mainFile = future.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            requestObserver.onCompleted();
            throw new RuntimeException("Timeout fetching file descriptor for " + grpcServiceName);
        }

        // Build the file descriptor with dependencies
        Descriptors.FileDescriptor fileDescriptor = buildFileDescriptor(mainFile, dependencyMap);
        fileDescriptorCache.put(cacheKey, fileDescriptor);
        
        return fileDescriptor;
    }

    /**
     * Build a FileDescriptor from proto with its dependencies.
     */
    private Descriptors.FileDescriptor buildFileDescriptor(
            DescriptorProtos.FileDescriptorProto fileProto,
            Map<String, DescriptorProtos.FileDescriptorProto> dependencyMap) throws Exception {
        
        List<Descriptors.FileDescriptor> dependencies = new ArrayList<>();
        
        for (String depName : fileProto.getDependencyList()) {
            DescriptorProtos.FileDescriptorProto depProto = dependencyMap.get(depName);
            if (depProto != null) {
                dependencies.add(buildFileDescriptor(depProto, dependencyMap));
            }
        }
        
        return Descriptors.FileDescriptor.buildFrom(
                fileProto,
                dependencies.toArray(new Descriptors.FileDescriptor[0])
        );
    }

    /**
     * Find a service descriptor by full name.
     */
    private Descriptors.ServiceDescriptor findService(Descriptors.FileDescriptor fileDescriptor, String serviceName) {
        // Try direct match
        for (Descriptors.ServiceDescriptor service : fileDescriptor.getServices()) {
            if (service.getFullName().equals(serviceName)) {
                return service;
            }
        }
        
        // Try matching by simple name (last part after dot)
        String simpleName = serviceName.contains(".") 
                ? serviceName.substring(serviceName.lastIndexOf('.') + 1) 
                : serviceName;
        
        for (Descriptors.ServiceDescriptor service : fileDescriptor.getServices()) {
            if (service.getName().equals(simpleName)) {
                return service;
            }
        }
        
        // Search in dependencies
        for (Descriptors.FileDescriptor dep : fileDescriptor.getDependencies()) {
            Descriptors.ServiceDescriptor found = findService(dep, serviceName);
            if (found != null) {
                return found;
            }
        }
        
        return null;
    }

    /**
     * Get a cache key for the channel (based on authority).
     */
    private String getChannelKey(Channel channel) {
        return channel.authority() != null ? channel.authority() : "unknown";
    }

    /**
     * Clear all caches (useful for testing or when services restart).
     */
    public void clearCache() {
        fileDescriptorCache.invalidateAll();
        methodCache.invalidateAll();
        log.info("Reflection cache cleared");
    }

    /**
     * Invalidate cache for a specific service (when it restarts).
     */
    public void invalidateService(String serviceName, String host, int port) {
        String channelKey = host + ":" + port;
        String cacheKey = serviceName + ":" + channelKey;
        fileDescriptorCache.invalidate(cacheKey);
        
        // Also invalidate method cache entries for this service
        methodCache.asMap().keySet().removeIf(key -> 
                key.startsWith(serviceName + "/") && key.endsWith(":" + channelKey));
        
        log.debug("Invalidated cache for service: {} at {}", serviceName, channelKey);
    }

    /**
     * Holds information about a gRPC method discovered via reflection.
     */
    public record MethodInfo(
            Descriptors.Descriptor inputType,
            Descriptors.Descriptor outputType,
            Descriptors.MethodDescriptor methodDescriptor
    ) {}
}




