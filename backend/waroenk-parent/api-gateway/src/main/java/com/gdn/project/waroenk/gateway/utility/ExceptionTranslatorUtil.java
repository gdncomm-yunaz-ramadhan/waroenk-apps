package com.gdn.project.waroenk.gateway.utility;

import com.gdn.project.waroenk.gateway.dto.ErrorResponseDto;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

/**
 * Utility class to translate gRPC exceptions to HTTP responses.
 * Based on the member service implementation for consistency.
 */
public final class ExceptionTranslatorUtil {

    private ExceptionTranslatorUtil() {
        // Utility class
    }

    public static ResponseEntity<ErrorResponseDto> translateGrpcRuntimeException(StatusRuntimeException exception) {
        Status status = exception.getStatus();
        HttpStatus httpStatus = convertToHttpStatus(status.getCode());
        String details = extractErrorDetails(status, exception);

        ErrorResponseDto errorResponse = new ErrorResponseDto(
                httpStatus.value(),
                httpStatus.name(),
                details,
                LocalDateTime.now());

        return ResponseEntity
                .status(httpStatus)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    /**
     * Extracts the error details from gRPC Status.
     * Priority: description > cause message > status code name
     */
    private static String extractErrorDetails(Status status, StatusRuntimeException exception) {
        // First, try to get the description from the status
        String description = status.getDescription();
        if (description != null && !description.isBlank()) {
            return description;
        }

        // Second, try to get the cause message
        Throwable cause = status.getCause();
        if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()) {
            return cause.getMessage();
        }

        // Third, try to parse the exception message (format: "STATUS_CODE: description")
        String exceptionMessage = exception.getMessage();
        if (exceptionMessage != null && !exceptionMessage.isBlank()) {
            int colonIndex = exceptionMessage.indexOf(':');
            if (colonIndex > 0 && colonIndex < exceptionMessage.length() - 1) {
                String parsedMessage = exceptionMessage.substring(colonIndex + 1).trim();
                if (!parsedMessage.isBlank()) {
                    return parsedMessage;
                }
            }
        }

        // Fallback: return a user-friendly message based on status code
        return getDefaultMessageForStatus(status.getCode());
    }

    /**
     * Returns a user-friendly default message for each gRPC status code
     */
    private static String getDefaultMessageForStatus(Status.Code code) {
        return switch (code) {
            case OK -> "Request completed successfully";
            case CANCELLED -> "Request was cancelled";
            case UNKNOWN -> "An unknown error occurred";
            case INVALID_ARGUMENT -> "Invalid request parameters";
            case DEADLINE_EXCEEDED -> "Request timed out";
            case NOT_FOUND -> "Resource not found";
            case ALREADY_EXISTS -> "Resource already exists";
            case PERMISSION_DENIED -> "Permission denied";
            case RESOURCE_EXHAUSTED -> "Resource quota exceeded";
            case FAILED_PRECONDITION -> "Operation cannot be performed in current state";
            case ABORTED -> "Operation was aborted";
            case OUT_OF_RANGE -> "Value out of valid range";
            case UNIMPLEMENTED -> "Operation not implemented";
            case INTERNAL -> "Internal server error";
            case UNAVAILABLE -> "Service temporarily unavailable";
            case DATA_LOSS -> "Data loss or corruption detected";
            case UNAUTHENTICATED -> "Authentication required";
        };
    }

    private static HttpStatus convertToHttpStatus(Status.Code code) {
        return switch (code) {
            case ABORTED -> HttpStatus.REQUEST_TIMEOUT;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case OK -> HttpStatus.OK;
            case INTERNAL, DATA_LOSS -> HttpStatus.INTERNAL_SERVER_ERROR;
            case INVALID_ARGUMENT, OUT_OF_RANGE, FAILED_PRECONDITION -> HttpStatus.BAD_REQUEST;
            case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case UNIMPLEMENTED -> HttpStatus.NOT_IMPLEMENTED;
            case RESOURCE_EXHAUSTED -> HttpStatus.TOO_MANY_REQUESTS;
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case PERMISSION_DENIED -> HttpStatus.FORBIDDEN;
            case ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case DEADLINE_EXCEEDED -> HttpStatus.GATEWAY_TIMEOUT;
            case CANCELLED -> HttpStatus.BAD_REQUEST;
            case UNKNOWN -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}











