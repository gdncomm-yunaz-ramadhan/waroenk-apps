package com.gdn.project.waroenk.cart.exceptions;

/**
 * Exception thrown when a user attempts to access a resource they don't own.
 */
public class AuthorizationException extends RuntimeException {
  
  public AuthorizationException(String message) {
    super(message);
  }

  public AuthorizationException(String message, Throwable cause) {
    super(message, cause);
  }
}




