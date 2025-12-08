package com.gdn.project.waroenk.gateway.exception;

/**
 * Base exception for gateway-related errors
 */
public class GatewayException extends RuntimeException {

    public GatewayException(String message) {
        super(message);
    }

    public GatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}











