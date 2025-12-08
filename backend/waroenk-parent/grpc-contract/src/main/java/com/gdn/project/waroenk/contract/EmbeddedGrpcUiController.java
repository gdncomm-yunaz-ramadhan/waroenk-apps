package com.gdn.project.waroenk.contract;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat;
import io.grpc.BindableService;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.protobuf.ProtoFileDescriptorSupplier;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.ClientCalls;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Embedded gRPC Web UI - No external tools required!
 * 
 * Provides a web interface to test gRPC methods directly from the browser.
 * Uses Server Reflection to discover services and methods.
 * 
 * Access: http://localhost:{port}/grpc-ui
 * 
 * Enable via: grpc.ui.enabled=true (default: true)
 */
@RestController
@ConditionalOnProperty(name = "grpc.ui.enabled", havingValue = "true", matchIfMissing = true)
public class EmbeddedGrpcUiController {

    private final List<BindableService> grpcServices;
    private final Map<String, Descriptors.MethodDescriptor> methodDescriptors = new HashMap<>();
    private final JsonFormat.Parser jsonParser = JsonFormat.parser().ignoringUnknownFields();
    private final JsonFormat.Printer jsonPrinter = JsonFormat.printer()
            .preservingProtoFieldNames()
            .includingDefaultValueFields();

    @Value("${spring.application.name:service}")
    private String appName;

    @Value("${info.app.version:1.0.0}")
    private String appVersion;

    @Value("${grpc.server.port:9090}")
    private int grpcPort;

    private ManagedChannel channel;
    private String htmlTemplate;

    @Autowired
    public EmbeddedGrpcUiController(List<BindableService> grpcServices) {
        this.grpcServices = grpcServices;
    }

    @PostConstruct
    public void init() {
        buildMethodDescriptorCache();
        loadHtmlTemplate();
    }

    private void loadHtmlTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource("grpc-ui/index.html");
            try (InputStream inputStream = resource.getInputStream()) {
                htmlTemplate = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            // Fallback to minimal HTML if resource not found
            htmlTemplate = """
                <!DOCTYPE html>
                <html>
                <head><title>gRPC UI - {{APP_NAME}}</title></head>
                <body>
                    <h1>gRPC UI - {{APP_NAME}}</h1>
                    <p>Error loading UI template. Check if grpc-ui/index.html exists in resources.</p>
                </body>
                </html>
                """;
        }
    }

    private void buildMethodDescriptorCache() {
        for (BindableService service : grpcServices) {
            ServerServiceDefinition definition = service.bindService();
            var schemaDescriptor = definition.getServiceDescriptor().getSchemaDescriptor();
            
            if (schemaDescriptor instanceof ProtoFileDescriptorSupplier) {
                Descriptors.FileDescriptor fileDescriptor = 
                        ((ProtoFileDescriptorSupplier) schemaDescriptor).getFileDescriptor();
                
                for (Descriptors.ServiceDescriptor serviceDesc : fileDescriptor.getServices()) {
                    for (Descriptors.MethodDescriptor methodDesc : serviceDesc.getMethods()) {
                        String key = serviceDesc.getFullName() + "/" + methodDesc.getName();
                        methodDescriptors.put(key, methodDesc);
                    }
                }
            }
        }
    }

    /**
     * Serve the gRPC UI HTML page
     */
    @GetMapping(value = "/grpc-ui", produces = MediaType.TEXT_HTML_VALUE)
    public String getGrpcUiPage() {
        return generateHtmlPage();
    }

    // Internal gRPC services to hide from the UI
    private static final List<String> HIDDEN_SERVICE_PREFIXES = List.of(
            "grpc.reflection",
            "grpc.health"
    );
    
    private boolean isHiddenService(String serviceName) {
        return HIDDEN_SERVICE_PREFIXES.stream()
                .anyMatch(prefix -> serviceName.startsWith(prefix));
    }

    /**
     * Get all available services and methods
     */
    @GetMapping(value = "/grpc-ui/services", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getServices() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> services = new ArrayList<>();

        for (BindableService service : grpcServices) {
            ServerServiceDefinition definition = service.bindService();
            var schemaDescriptor = definition.getServiceDescriptor().getSchemaDescriptor();
            
            if (schemaDescriptor instanceof ProtoFileDescriptorSupplier) {
                Descriptors.FileDescriptor fileDescriptor = 
                        ((ProtoFileDescriptorSupplier) schemaDescriptor).getFileDescriptor();
                
                for (Descriptors.ServiceDescriptor serviceDesc : fileDescriptor.getServices()) {
                    // Skip internal gRPC services
                    if (isHiddenService(serviceDesc.getFullName())) {
                        continue;
                    }
                    
                    Map<String, Object> serviceInfo = new LinkedHashMap<>();
                    serviceInfo.put("name", serviceDesc.getFullName());
                    
                    List<Map<String, Object>> methods = new ArrayList<>();
                    for (Descriptors.MethodDescriptor methodDesc : serviceDesc.getMethods()) {
                        Map<String, Object> methodInfo = new LinkedHashMap<>();
                        methodInfo.put("name", methodDesc.getName());
                        methodInfo.put("fullName", serviceDesc.getFullName() + "/" + methodDesc.getName());
                        methodInfo.put("inputType", methodDesc.getInputType().getFullName());
                        methodInfo.put("outputType", methodDesc.getOutputType().getFullName());
                        methodInfo.put("requestSchema", getMessageSchema(methodDesc.getInputType()));
                        methodInfo.put("requestExample", getMessageExample(methodDesc.getInputType()));
                        methodInfo.put("responseSchema", getMessageSchema(methodDesc.getOutputType()));
                        methodInfo.put("responseExample", getMessageExample(methodDesc.getOutputType()));
                        methods.add(methodInfo);
                    }
                    
                    serviceInfo.put("methods", methods);
                    services.add(serviceInfo);
                }
            }
        }

        result.put("services", services);
        result.put("appName", appName);
        result.put("appVersion", appVersion);
        result.put("grpcPort", grpcPort);
        return ResponseEntity.ok(result);
    }

    /**
     * Invoke a gRPC method
     */
    @PostMapping(value = "/grpc-ui/invoke", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> invokeMethod(
            @RequestParam String method,
            @RequestBody String jsonRequest) {
        
        Map<String, Object> result = new LinkedHashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            Descriptors.MethodDescriptor methodDesc = methodDescriptors.get(method);
            if (methodDesc == null) {
                result.put("success", false);
                result.put("error", "Method not found: " + method);
                return ResponseEntity.badRequest().body(result);
            }

            // Parse JSON to DynamicMessage
            DynamicMessage.Builder requestBuilder = DynamicMessage.newBuilder(methodDesc.getInputType());
            jsonParser.merge(jsonRequest, requestBuilder);
            DynamicMessage request = requestBuilder.build();

            // Get or create channel
            if (channel == null || channel.isShutdown()) {
                channel = ManagedChannelBuilder.forAddress("localhost", grpcPort)
                        .usePlaintext()
                        .build();
            }

            // Build method descriptor for the call
            String fullMethodName = methodDesc.getService().getFullName() + "/" + methodDesc.getName();
            MethodDescriptor<DynamicMessage, DynamicMessage> grpcMethodDesc = MethodDescriptor
                    .<DynamicMessage, DynamicMessage>newBuilder()
                    .setType(MethodDescriptor.MethodType.UNARY)
                    .setFullMethodName(fullMethodName)
                    .setRequestMarshaller(ProtoUtils.marshaller(DynamicMessage.getDefaultInstance(methodDesc.getInputType())))
                    .setResponseMarshaller(ProtoUtils.marshaller(DynamicMessage.getDefaultInstance(methodDesc.getOutputType())))
                    .build();

            // Make the call
            DynamicMessage response = ClientCalls.blockingUnaryCall(
                    channel,
                    grpcMethodDesc,
                    CallOptions.DEFAULT,
                    request
            );

            long duration = System.currentTimeMillis() - startTime;

            result.put("success", true);
            result.put("response", jsonPrinter.print(response));
            result.put("duration", duration + "ms");
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("errorType", e.getClass().getSimpleName());
            result.put("duration", duration + "ms");
        }

        return ResponseEntity.ok(result);
    }

    private Map<String, Object> getMessageSchema(Descriptors.Descriptor descriptor) {
        return getMessageSchema(descriptor, new HashSet<>(), 0);
    }
    
    private Map<String, Object> getMessageSchema(Descriptors.Descriptor descriptor, Set<String> visited, int depth) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("name", descriptor.getName());
        
        // Prevent infinite recursion with max depth and visited tracking
        if (depth > 5 || visited.contains(descriptor.getFullName())) {
            schema.put("fields", List.of());
            schema.put("truncated", true);
            return schema;
        }
        
        visited.add(descriptor.getFullName());
        
        List<Map<String, Object>> fields = new ArrayList<>();
        for (Descriptors.FieldDescriptor field : descriptor.getFields()) {
            Map<String, Object> fieldInfo = new LinkedHashMap<>();
            fieldInfo.put("name", field.getName());
            fieldInfo.put("type", getFieldTypeName(field));
            fieldInfo.put("repeated", field.isRepeated());
            
            if (field.getType() == Descriptors.FieldDescriptor.Type.MESSAGE) {
                fieldInfo.put("nested", getMessageSchema(field.getMessageType(), new HashSet<>(visited), depth + 1));
            }
            
            fields.add(fieldInfo);
        }
        
        schema.put("fields", fields);
        return schema;
    }

    private String getMessageExample(Descriptors.Descriptor descriptor) {
        return getMessageExample(descriptor, new HashSet<>(), 0);
    }
    
    private String getMessageExample(Descriptors.Descriptor descriptor, Set<String> visited, int depth) {
        // Prevent infinite recursion
        if (depth > 3 || visited.contains(descriptor.getFullName())) {
            return "{}";
        }
        visited.add(descriptor.getFullName());
        
        StringBuilder sb = new StringBuilder("{\n");
        List<Descriptors.FieldDescriptor> fields = descriptor.getFields();
        
        for (int i = 0; i < fields.size(); i++) {
            Descriptors.FieldDescriptor field = fields.get(i);
            sb.append("  \"").append(field.getName()).append("\": ");
            sb.append(getFieldExample(field, new HashSet<>(visited), depth));
            if (i < fields.size() - 1) sb.append(",");
            sb.append("\n");
        }
        
        sb.append("}");
        return sb.toString();
    }

    private String getFieldExample(Descriptors.FieldDescriptor field, Set<String> visited, int depth) {
        if (field.isRepeated()) {
            return "[]";
        }
        
        return switch (field.getType()) {
            case STRING -> "\"\"";
            case INT32, INT64, SINT32, SINT64, UINT32, UINT64, FIXED32, FIXED64, SFIXED32, SFIXED64 -> "0";
            case FLOAT, DOUBLE -> "0.0";
            case BOOL -> "false";
            case BYTES -> "\"\"";
            case ENUM -> "\"" + field.getEnumType().getValues().get(0).getName() + "\"";
            case MESSAGE -> getMessageExample(field.getMessageType(), visited, depth + 1);
            default -> "null";
        };
    }

    private String getFieldTypeName(Descriptors.FieldDescriptor field) {
        return switch (field.getType()) {
            case MESSAGE -> field.getMessageType().getName();
            case ENUM -> field.getEnumType().getName();
            default -> field.getType().name().toLowerCase();
        };
    }

    private String generateHtmlPage() {
        return htmlTemplate.replace("{{APP_NAME}}", appName);
    }
}

