package com.gdn.project.waroenk.contract;

import com.gdn.project.waroenk.contract.annotation.GatewayRoute;
import com.gdn.project.waroenk.contract.annotation.GatewayService;
import io.grpc.BindableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Auto-discovers gRPC routes from annotated service implementations.
 * Scans for @GatewayService and @GatewayRoute annotations to build
 * route definitions for API Gateway registration.
 * 
 * <p>Usage:</p>
 * <pre>
 * {@code
 * GatewayAutoDiscoverer discoverer = new GatewayAutoDiscoverer();
 * List<GatewayRegistrationClient.Route> routes = discoverer.discoverRoutes(grpcServices);
 * }
 * </pre>
 */
public class GatewayAutoDiscoverer {

    private static final Logger log = LoggerFactory.getLogger(GatewayAutoDiscoverer.class);
    private static final Pattern GRPC_STUB_PATTERN = Pattern.compile("(.+)Grpc\\$(.+)ImplBase");

    /**
     * Discover all routes from a collection of gRPC services.
     * Services must be annotated with @GatewayService and methods with @GatewayRoute.
     * 
     * @param services Collection of gRPC BindableService implementations
     * @return List of discovered routes ready for registration
     */
    public List<GatewayRegistrationClient.Route> discoverRoutes(Collection<? extends BindableService> services) {
        List<GatewayRegistrationClient.Route> allRoutes = new ArrayList<>();

        for (BindableService service : services) {
            try {
                List<GatewayRegistrationClient.Route> serviceRoutes = discoverRoutesFromService(service);
                allRoutes.addAll(serviceRoutes);
            } catch (Exception e) {
                log.warn("Failed to discover routes from service {}: {}", 
                        service.getClass().getSimpleName(), e.getMessage());
            }
        }

        log.info("Discovered {} routes from {} services", allRoutes.size(), services.size());
        return allRoutes;
    }

    /**
     * Discover routes from a single gRPC service.
     */
    public List<GatewayRegistrationClient.Route> discoverRoutesFromService(BindableService service) {
        List<GatewayRegistrationClient.Route> routes = new ArrayList<>();
        Class<?> serviceClass = service.getClass();

        // Get the gRPC service name
        String grpcServiceName = getGrpcServiceName(serviceClass);
        if (grpcServiceName == null) {
            log.debug("Skipping service {} - no @GatewayService annotation or unable to determine service name",
                    serviceClass.getSimpleName());
            return routes;
        }

        // Scan methods for @GatewayRoute
        for (Method method : serviceClass.getDeclaredMethods()) {
            GatewayRoute routeAnnotation = method.getAnnotation(GatewayRoute.class);
            if (routeAnnotation == null) {
                continue;
            }

            try {
                GatewayRegistrationClient.Route route = buildRoute(method, routeAnnotation, grpcServiceName);
                routes.add(route);
                log.debug("Discovered route: {} {} -> {}.{}", 
                        route.httpMethod(), route.path(), grpcServiceName, route.grpcMethod());
            } catch (Exception e) {
                log.warn("Failed to build route for method {}: {}", method.getName(), e.getMessage());
            }
        }

        return routes;
    }

    /**
     * Get the gRPC service name from a service implementation class.
     */
    private String getGrpcServiceName(Class<?> serviceClass) {
        // First, check for @GatewayService annotation
        GatewayService annotation = serviceClass.getAnnotation(GatewayService.class);
        if (annotation != null && !annotation.name().isEmpty()) {
            return annotation.name();
        }

        // Try to derive from parent class name (e.g., UserServiceGrpc.UserServiceImplBase)
        Class<?> superClass = serviceClass.getSuperclass();
        while (superClass != null && superClass != Object.class) {
            String className = superClass.getName();
            Matcher matcher = GRPC_STUB_PATTERN.matcher(className);
            if (matcher.find()) {
                // Extract package and service name
                String fullName = matcher.group(1);
                int lastDot = fullName.lastIndexOf('.');
                if (lastDot > 0) {
                    String pkg = fullName.substring(0, lastDot);
                    String serviceName = fullName.substring(lastDot + 1);
                    return pkg + "." + serviceName;
                }
                return fullName;
            }
            superClass = superClass.getSuperclass();
        }

        return null;
    }

    /**
     * Build a route from a method and its annotation.
     */
    private GatewayRegistrationClient.Route buildRoute(Method method, GatewayRoute annotation, 
                                                        String grpcServiceName) {
        // Get request and response types from method signature
        String requestType = null;
        String responseType = null;

        Class<?>[] paramTypes = method.getParameterTypes();
        Type[] genericParamTypes = method.getGenericParameterTypes();

        // gRPC methods typically have (RequestType, StreamObserver<ResponseType>)
        if (paramTypes.length >= 1) {
            requestType = getFullTypeName(paramTypes[0]);
        }

        if (paramTypes.length >= 2 && genericParamTypes.length >= 2) {
            Type observerType = genericParamTypes[1];
            if (observerType instanceof ParameterizedType pt) {
                Type[] typeArgs = pt.getActualTypeArguments();
                if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> respClass) {
                    responseType = getFullTypeName(respClass);
                }
            }
        }

        // Capitalize method name for gRPC convention
        String grpcMethodName = capitalizeFirst(method.getName());

        return new GatewayRegistrationClient.Route(
                annotation.method().name(),
                annotation.path(),
                grpcServiceName,
                grpcMethodName,
                requestType,
                responseType,
                annotation.publicEndpoint(),
                Arrays.asList(annotation.requiredRoles())
        );
    }

    /**
     * Get the fully qualified type name for Protobuf messages.
     */
    private String getFullTypeName(Class<?> clazz) {
        // For Protobuf messages, return the full class name
        if (com.google.protobuf.Message.class.isAssignableFrom(clazz)) {
            return clazz.getName();
        }
        return clazz.getName();
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}

