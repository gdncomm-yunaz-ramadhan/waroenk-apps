package com.gdn.project.waroenk.gateway.exception;

/**
 * Exception thrown when authentication fails
 */
public class AuthenticationException extends GatewayException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}











