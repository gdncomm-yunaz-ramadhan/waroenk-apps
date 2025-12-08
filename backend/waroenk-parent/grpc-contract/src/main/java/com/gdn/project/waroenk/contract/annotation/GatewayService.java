package com.gdn.project.waroenk.contract.annotation;

import java.lang.annotation.*;

/**
 * Annotation to mark a gRPC service implementation for gateway auto-discovery.
 * Place this on your gRPC service implementation class along with @GrpcService.
 * 
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * @GrpcService
 * @GatewayService(name = "member.user.UserService")
 * public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {
 *     // ...
 * }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GatewayService {
    
    /**
     * Full gRPC service name (e.g., "member.user.UserService").
     * If empty, will be derived from the class hierarchy.
     */
    String name() default "";
}









