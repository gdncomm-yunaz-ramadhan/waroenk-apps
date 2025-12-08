package com.gdn.project.waroenk.contract.annotation;

import java.lang.annotation.*;

/**
 * Annotation to define HTTP routing for a gRPC method.
 * Apply this to gRPC service implementation methods to enable automatic 
 * route discovery and registration with the API Gateway.
 * 
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * @GrpcService
 * public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {
 *     
 *     @GatewayRoute(
 *         method = HttpMethod.POST, 
 *         path = "/api/user/register",
 *         publicEndpoint = true
 *     )
 *     @Override
 *     public void register(CreateUserRequest request, StreamObserver<CreateUserResponse> observer) {
 *         // ...
 *     }
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GatewayRoute {
    
    /**
     * HTTP method for this route
     */
    HttpMethod method();
    
    /**
     * HTTP path pattern (e.g., "/api/user/{id}")
     */
    String path();
    
    /**
     * Whether this endpoint is public (no authentication required)
     */
    boolean publicEndpoint() default false;
    
    /**
     * Required roles to access this endpoint.
     * Empty means any authenticated user can access.
     */
    String[] requiredRoles() default {};
    
    /**
     * Description for API documentation
     */
    String description() default "";
    
    /**
     * HTTP methods enum
     */
    enum HttpMethod {
        GET, POST, PUT, DELETE, PATCH
    }
}









