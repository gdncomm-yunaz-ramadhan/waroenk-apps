package com.gdn.project.waroenk.cart.utility;

import com.gdn.project.waroenk.cart.dto.ErrorResponseDto;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

public class ExceptionTranslatorUtil {

    public static ResponseEntity<ErrorResponseDto> translateGrpcRuntimeException(StatusRuntimeException ex) {
        Status.Code code = ex.getStatus().getCode();
        String description = ex.getStatus().getDescription();

        HttpStatus httpStatus;
        String message;

        switch (code) {
            case INVALID_ARGUMENT -> {
                httpStatus = HttpStatus.BAD_REQUEST;
                message = description != null ? description : "Invalid argument";
            }
            case NOT_FOUND -> {
                httpStatus = HttpStatus.NOT_FOUND;
                message = description != null ? description : "Resource not found";
            }
            case ALREADY_EXISTS -> {
                httpStatus = HttpStatus.CONFLICT;
                message = description != null ? description : "Resource already exists";
            }
            case PERMISSION_DENIED -> {
                httpStatus = HttpStatus.FORBIDDEN;
                message = description != null ? description : "Permission denied";
            }
            case UNAUTHENTICATED -> {
                httpStatus = HttpStatus.UNAUTHORIZED;
                message = description != null ? description : "Authentication required";
            }
            case FAILED_PRECONDITION -> {
                httpStatus = HttpStatus.PRECONDITION_FAILED;
                message = description != null ? description : "Precondition failed";
            }
            case UNAVAILABLE -> {
                httpStatus = HttpStatus.SERVICE_UNAVAILABLE;
                message = description != null ? description : "Service unavailable";
            }
            default -> {
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
                message = description != null ? description : "Internal server error";
            }
        }

        ErrorResponseDto errorResponse = new ErrorResponseDto(
                httpStatus.value(),
                httpStatus.name(),
                message,
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(httpStatus)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }
}
