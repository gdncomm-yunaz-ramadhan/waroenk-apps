package com.gdn.project.waroenk.contract;

import com.google.protobuf.Descriptors;
import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.protobuf.ProtoFileDescriptorSupplier;

import java.util.*;

/**
 * Utility to generate documentation from gRPC service definitions.
 * Uses reflection to discover all registered gRPC services and their methods.
 * 
 * Usage in a microservice:
 * <pre>
 * {@code
 * @RestController
 * public class GrpcDocsController {
 *     @Autowired
 *     private List<BindableService> grpcServices;
 *     
 *     @GetMapping("/grpc-docs")
 *     public Map<String, Object> getGrpcDocs() {
 *         return GrpcDocumentationService.generateDocs(grpcServices);
 *     }
 * }
 * }
 * </pre>
 */
public class GrpcDocumentationService {

    /**
     * Generate documentation for all gRPC services.
     * 
     * @param grpcServices List of registered gRPC services
     * @return Documentation as a map (can be serialized to JSON)
     */
    public static Map<String, Object> generateDocs(Collection<BindableService> grpcServices) {
        Map<String, Object> docs = new LinkedHashMap<>();
        List<Map<String, Object>> services = new ArrayList<>();

        for (BindableService service : grpcServices) {
            ServerServiceDefinition definition = service.bindService();
            Map<String, Object> serviceDoc = new LinkedHashMap<>();
            
            String serviceName = definition.getServiceDescriptor().getName();
            serviceDoc.put("name", serviceName);
            
            // Get methods
            List<Map<String, Object>> methods = new ArrayList<>();
            definition.getMethods().forEach(method -> {
                Map<String, Object> methodDoc = new LinkedHashMap<>();
                String fullMethodName = method.getMethodDescriptor().getFullMethodName();
                String methodName = fullMethodName.contains("/") 
                        ? fullMethodName.substring(fullMethodName.indexOf('/') + 1)
                        : fullMethodName;
                
                methodDoc.put("name", methodName);
                methodDoc.put("full_name", fullMethodName);
                methodDoc.put("type", method.getMethodDescriptor().getType().name());
                
                // Try to get request/response type names
                try {
                    var descriptor = method.getMethodDescriptor();
                    methodDoc.put("request_type", getMessageTypeName(descriptor.getRequestMarshaller()));
                    methodDoc.put("response_type", getMessageTypeName(descriptor.getResponseMarshaller()));
                } catch (Exception e) {
                    // Ignore if we can't get type info
                }
                
                methods.add(methodDoc);
            });
            
            serviceDoc.put("methods", methods);
            serviceDoc.put("method_count", methods.size());
            services.add(serviceDoc);
        }

        docs.put("services", services);
        docs.put("service_count", services.size());
        docs.put("total_methods", services.stream()
                .mapToInt(s -> (int) s.get("method_count"))
                .sum());
        
        return docs;
    }

    /**
     * Generate detailed documentation including message field descriptions.
     */
    public static Map<String, Object> generateDetailedDocs(Collection<BindableService> grpcServices) {
        Map<String, Object> docs = generateDocs(grpcServices);
        
        // Add message type details
        Map<String, Object> messageTypes = new LinkedHashMap<>();
        
        for (BindableService service : grpcServices) {
            try {
                ServerServiceDefinition definition = service.bindService();
                var schemaSupplier = definition.getServiceDescriptor().getSchemaDescriptor();
                
                if (schemaSupplier instanceof ProtoFileDescriptorSupplier) {
                    Descriptors.FileDescriptor fileDescriptor = 
                            ((ProtoFileDescriptorSupplier) schemaSupplier).getFileDescriptor();
                    
                    // Get all message types from the file
                    for (Descriptors.Descriptor messageType : fileDescriptor.getMessageTypes()) {
                        messageTypes.put(messageType.getFullName(), describeMessage(messageType));
                    }
                }
            } catch (Exception e) {
                // Skip if we can't get detailed info
            }
        }
        
        docs.put("message_types", messageTypes);
        return docs;
    }

    private static Map<String, Object> describeMessage(Descriptors.Descriptor descriptor) {
        Map<String, Object> messageDoc = new LinkedHashMap<>();
        messageDoc.put("name", descriptor.getName());
        messageDoc.put("full_name", descriptor.getFullName());
        
        List<Map<String, Object>> fields = new ArrayList<>();
        for (Descriptors.FieldDescriptor field : descriptor.getFields()) {
            Map<String, Object> fieldDoc = new LinkedHashMap<>();
            fieldDoc.put("name", field.getName());
            fieldDoc.put("number", field.getNumber());
            fieldDoc.put("type", field.getType().name());
            fieldDoc.put("label", field.isRepeated() ? "repeated" : field.isRequired() ? "required" : "optional");
            
            if (field.getType() == Descriptors.FieldDescriptor.Type.MESSAGE) {
                fieldDoc.put("message_type", field.getMessageType().getFullName());
            } else if (field.getType() == Descriptors.FieldDescriptor.Type.ENUM) {
                fieldDoc.put("enum_type", field.getEnumType().getFullName());
            }
            
            fields.add(fieldDoc);
        }
        
        messageDoc.put("fields", fields);
        return messageDoc;
    }

    private static String getMessageTypeName(io.grpc.MethodDescriptor.Marshaller<?> marshaller) {
        // Try to extract type name from marshaller
        String className = marshaller.getClass().getName();
        if (className.contains("$")) {
            return className.substring(className.lastIndexOf('$') + 1);
        }
        return "unknown";
    }
}

