package com.esign.exception;

import com.esign.constant.StatusMessage;
import com.esign.entities.WebErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<WebErrorResponse<String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage() != null ? ex.getMessage() : "Invalid argument");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<WebErrorResponse<String>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        return createErrorResponse(HttpStatus.BAD_REQUEST, "Invalid format input: " + ex.getMostSpecificCause().getMessage());
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<WebErrorResponse<String>> handlePropertyReferenceException(PropertyReferenceException ex) {
        return createErrorResponse(HttpStatus.BAD_REQUEST, "Invalid sort field: " + ex.getPropertyName());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<WebErrorResponse<String>> handleIllegalStateException(IllegalStateException ex) {
        return createErrorResponse(HttpStatus.BAD_REQUEST, ex != null ? ex.getMessage() : "");
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<WebErrorResponse<String>> handleNullPointerException(NullPointerException ex) {
        return createErrorResponse(HttpStatus.BAD_REQUEST, ex != null ? ex.getMessage() : "");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<WebErrorResponse<Object>> handleConstraintViolationException(ConstraintViolationException exception) {
        List<Map<String, String>> errors = exception.getConstraintViolations().stream()
                .map(violation -> Map.of("path", violation.getPropertyPath().toString(), "message", violation.getMessage()))
                .collect(Collectors.toList());
        return createErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, errors);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<WebErrorResponse<String>> handleNotFoundException(NotFoundException exception) {
        return createErrorResponse(HttpStatus.NOT_FOUND, exception.getMessage() != null ? exception.getMessage() : "Not Found");
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<WebErrorResponse<String>> handleUnauthorizedException(UnauthorizedException exception) {
        return createErrorResponse(HttpStatus.UNAUTHORIZED, exception.getMessage() != null ? exception.getMessage() : "Unauthorized");
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<WebErrorResponse<String>> handleBadRequestException(BadRequestException exception) {
        return createErrorResponse(HttpStatus.BAD_REQUEST, exception.getMessage() != null ? exception.getMessage() : "Bad Request");
    }

    @ExceptionHandler(ValidationCustomException.class)
    public ResponseEntity<WebErrorResponse<Object>> handleValidationCustomException(ValidationCustomException exception) {
        List<Map<String, String>> errors = List.of(Map.of("path", exception.getPath(), "message", exception.getMessage()));
        return createErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, errors);
    }

    @ExceptionHandler(InternalServerException.class)
    public ResponseEntity<WebErrorResponse<String>> handleInternalServerException(InternalServerException ex) {
        log.error("Unhandled ex: {}", ex.getMessage(), ex);
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage() != null ? ex.getMessage() : StatusMessage.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<WebErrorResponse<String>> handleAccessDeniedException(AccessDeniedException ex) {
        return createErrorResponse(HttpStatus.FORBIDDEN, ex != null ? ex.getMessage() : "");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<WebErrorResponse<String>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String errorMessage = StatusMessage.BAD_REQUEST;
        return createErrorResponse(HttpStatus.BAD_REQUEST, errorMessage);
    }

    private <T> ResponseEntity<WebErrorResponse<T>> createErrorResponse(HttpStatus status, T data) {
        WebErrorResponse<T> response = new WebErrorResponse<>();
        response.setCode(status.value());
        response.setStatus(status.name());
        response.setMessages(data);
        return ResponseEntity.status(status).body(response);
    }
}

