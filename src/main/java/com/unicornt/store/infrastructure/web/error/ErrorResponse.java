package com.unicornt.store.infrastructure.web.error;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

/** Single error payload returned by every failing endpoint. */
@Schema(name = "ErrorResponse", description = "Uniform error payload returned by the API")
public record ErrorResponse(

        @Schema(description = "Human readable error message", example = "Product not found: 42")
        String message,

        @Schema(description = "Stable machine readable error code", example = "RESOURCE_NOT_FOUND")
        String code,

        @Schema(description = "HTTP status code", example = "404")
        int status,

        @Schema(description = "Moment the error was produced, in UTC")
        Instant timestamp,

        @Schema(description = "Request path that produced the error", example = "/api/v1/products/42")
        String path,

        @Schema(description = "Field level errors, empty unless the failure is a validation failure")
        List<FieldError> errors) {

    /** One rejected field of a validated request body or parameter. */
    @Schema(name = "FieldError", description = "A single rejected field")
    public record FieldError(

            @Schema(description = "Rejected field name", example = "price")
            String field,

            @Schema(description = "Reason the field was rejected", example = "must be greater than 0")
            String message) {
    }

    /** Builds a response with no field level detail. */
    public static ErrorResponse of(String msg, String code, HttpStatus status, String path) {
        return new ErrorResponse(msg, code, status.value(), Instant.now(), path, List.of());
    }
}
