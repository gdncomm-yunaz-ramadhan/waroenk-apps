package com.gdn.project.waroenk.gateway.exception;

/**
 * Exception thrown when a backend service is unavailable
 */
public class ServiceUnavailableException extends GatewayException {

    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}










