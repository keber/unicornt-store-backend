package com.unicornt.store.infrastructure.web.error;

import com.unicornt.store.domain.exception.DuplicateResourceException;
import com.unicornt.store.domain.exception.OutOfStockException;
import com.unicornt.store.domain.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;

/**
 * Single translation point from exception to JSON error payload.
 * No stack trace ever reaches the client; unexpected failures are logged server side.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ErrorResponse> notFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return build(ex.getMessage(), "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, req);
    }

    @ExceptionHandler(OutOfStockException.class)
    ResponseEntity<ErrorResponse> business(OutOfStockException ex, HttpServletRequest req) {
        return build(ex.getMessage(), "BUSINESS_RULE_VIOLATION", HttpStatus.UNPROCESSABLE_ENTITY, req);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    ResponseEntity<ErrorResponse> conflict(DuplicateResourceException ex, HttpServletRequest req) {
        return build(ex.getMessage(), "RESOURCE_CONFLICT", HttpStatus.CONFLICT, req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ErrorResponse.FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new ErrorResponse.FieldError(f.getField(), f.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(new ErrorResponse(
                "Validation failed", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST.value(),
                Instant.now(), req.getRequestURI(), fields));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> constraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        List<ErrorResponse.FieldError> fields = ex.getConstraintViolations().stream()
                .map(v -> new ErrorResponse.FieldError(
                        v.getPropertyPath() != null ? v.getPropertyPath().toString() : "",
                        v.getMessage()))
                .toList();
        return ResponseEntity.badRequest().body(new ErrorResponse(
                "Validation failed", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST.value(),
                Instant.now(), req.getRequestURI(), fields));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> malformedBody(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build("Malformed request body", "MALFORMED_REQUEST", HttpStatus.BAD_REQUEST, req);
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorResponse> badRequest(Exception ex, HttpServletRequest req) {
        return build(ex.getMessage() != null ? ex.getMessage() : "Invalid request",
                "BAD_REQUEST", HttpStatus.BAD_REQUEST, req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> dataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Data integrity violation at {}", req.getRequestURI(), ex);
        return build("The request conflicts with the current state of the resource",
                "RESOURCE_CONFLICT", HttpStatus.CONFLICT, req);
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    ResponseEntity<ErrorResponse> noHandler(Exception ex, HttpServletRequest req) {
        return build("No endpoint " + req.getMethod() + " " + req.getRequestURI(),
                "ENDPOINT_NOT_FOUND", HttpStatus.NOT_FOUND, req);
    }

    /**
     * Authorization failures raised inside the dispatcher (method security) must keep their
     * 403 semantics instead of falling through to the generic safety net below.
     * Failures raised in the filter chain are handled by the security entry points (T2).
     */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> accessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return build("Access denied", "ACCESS_DENIED", HttpStatus.FORBIDDEN, req);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErrorResponse> unauthenticated(AuthenticationException ex, HttpServletRequest req) {
        return build("Authentication required", "UNAUTHORIZED", HttpStatus.UNAUTHORIZED, req);
    }

    /** Safety net: never leak a stack trace to the client. */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> generic(Exception ex, HttpServletRequest req) {
        log.error("Unhandled error at {}", req.getRequestURI(), ex);
        return build("Internal error", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, req);
    }

    private ResponseEntity<ErrorResponse> build(String message, String code,
                                                HttpStatus status, HttpServletRequest req) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(message, code, status, req.getRequestURI()));
    }
}
