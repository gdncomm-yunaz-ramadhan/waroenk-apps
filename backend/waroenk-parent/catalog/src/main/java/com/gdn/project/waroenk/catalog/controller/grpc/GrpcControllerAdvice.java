package com.gdn.project.waroenk.catalog.controller.grpc;

import com.gdn.project.waroenk.catalog.exceptions.DuplicateResourceException;
import com.gdn.project.waroenk.catalog.exceptions.ResourceNotFoundException;
import com.gdn.project.waroenk.catalog.exceptions.ValidationException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.stream.Collectors;

@Slf4j
@GrpcAdvice
public class GrpcControllerAdvice {

  // ==================== Client Errors (4xx equivalent) ====================

  @GrpcExceptionHandler(IllegalArgumentException.class)
  public StatusRuntimeException handleIllegalArgumentException(IllegalArgumentException ex) {
    log.warn("Invalid argument: {}", ex.getMessage());
    return Status.INVALID_ARGUMENT.withDescription(ex.getMessage()).asRuntimeException();
  }

  @GrpcExceptionHandler(ValidationException.class)
  public StatusRuntimeException handleValidationException(ValidationException ex) {
    log.warn("Validation error: {}", ex.getMessage());
    return Status.INVALID_ARGUMENT.withDescription(ex.getMessage()).asRuntimeException();
  }

  @GrpcExceptionHandler(ConstraintViolationException.class)
  public StatusRuntimeException handleConstraintViolationException(ConstraintViolationException ex) {
    String violations =
        ex.getConstraintViolations().stream().map(ConstraintViolation::getMessage).collect(Collectors.joining(", "));
    log.warn("Constraint violation: {}", violations);
    return Status.INVALID_ARGUMENT.withDescription(violations).asRuntimeException();
  }

  @GrpcExceptionHandler(ResourceNotFoundException.class)
  public StatusRuntimeException handleResourceNotFoundException(ResourceNotFoundException ex) {
    log.warn("Resource not found: {}", ex.getMessage());
    return Status.NOT_FOUND.withDescription(ex.getMessage()).asRuntimeException();
  }

  @GrpcExceptionHandler(DuplicateResourceException.class)
  public StatusRuntimeException handleDuplicateResourceException(DuplicateResourceException ex) {
    log.warn("Duplicate resource: {}", ex.getMessage());
    return Status.ALREADY_EXISTS.withDescription(ex.getMessage()).asRuntimeException();
  }

  @GrpcExceptionHandler(DataIntegrityViolationException.class)
  public StatusRuntimeException handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
    log.warn("Data integrity violation: {}", ex.getMessage());
    String message = "Data conflict occurred";
    if (ex.getMessage() != null) {
      if (ex.getMessage().contains("duplicate") || ex.getMessage().contains("unique")) {
        message = "Resource already exists";
      } else if (ex.getMessage().contains("foreign key") || ex.getMessage().contains("fk_")) {
        message = "Referenced resource not found or cannot be deleted";
      }
    }
    return Status.ALREADY_EXISTS.withDescription(message).asRuntimeException();
  }

  // ==================== Server Errors (5xx equivalent) ====================

  @GrpcExceptionHandler(IllegalStateException.class)
  public StatusRuntimeException handleIllegalStateException(IllegalStateException ex) {
    log.error("Illegal state: {}", ex.getMessage(), ex);
    return Status.FAILED_PRECONDITION.withDescription("Operation cannot be performed in current state")
        .asRuntimeException();
  }

  @GrpcExceptionHandler(NullPointerException.class)
  public StatusRuntimeException handleNullPointerException(NullPointerException ex) {
    log.error("Null pointer exception", ex);
    return Status.INTERNAL.withDescription("An internal error occurred").asRuntimeException();
  }

  @GrpcExceptionHandler(RuntimeException.class)
  public StatusRuntimeException handleRuntimeException(RuntimeException ex) {
    log.error("Runtime exception: {}", ex.getMessage(), ex);
    return Status.INTERNAL.withDescription("An internal error occurred: " + ex.getMessage()).asRuntimeException();
  }

  @GrpcExceptionHandler(Exception.class)
  public StatusRuntimeException handleGenericException(Exception ex) {
    log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
    return Status.INTERNAL.withDescription("An unexpected error occurred").asRuntimeException();
  }
}
