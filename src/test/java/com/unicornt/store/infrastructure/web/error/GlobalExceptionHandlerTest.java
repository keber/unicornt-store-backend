package com.unicornt.store.infrastructure.web.error;

import com.unicornt.store.domain.exception.DuplicateResourceException;
import com.unicornt.store.domain.exception.OutOfStockException;
import com.unicornt.store.domain.exception.ResourceNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every branch of {@link GlobalExceptionHandler}: each handler is called directly with its
 * exception and a {@link MockHttpServletRequest}, and the resulting {@link ResponseEntity}
 * status and {@link ErrorResponse#code()} are asserted, plus the field errors where relevant.
 */
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest("GET", "/api/v1/things/42");
    }

    private static ErrorResponse bodyOf(ResponseEntity<ErrorResponse> response) {
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        return body;
    }

    @Nested
    @DisplayName("domain exceptions")
    class DomainExceptions {

        @Test
        @DisplayName("ResourceNotFoundException maps to 404 RESOURCE_NOT_FOUND")
        void resourceNotFound() {
            ResponseEntity<ErrorResponse> response =
                    handler.notFound(new ResourceNotFoundException("Product", 42), request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(bodyOf(response).code()).isEqualTo("RESOURCE_NOT_FOUND");
            assertThat(bodyOf(response).path()).isEqualTo("/api/v1/things/42");
        }

        @Test
        @DisplayName("OutOfStockException maps to 422 BUSINESS_RULE_VIOLATION")
        void outOfStock() {
            ResponseEntity<ErrorResponse> response =
                    handler.business(new OutOfStockException(7), request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(bodyOf(response).code()).isEqualTo("BUSINESS_RULE_VIOLATION");
        }

        @Test
        @DisplayName("DuplicateResourceException maps to 409 RESOURCE_CONFLICT")
        void duplicateResource() {
            ResponseEntity<ErrorResponse> response =
                    handler.conflict(new DuplicateResourceException("User", "email", "a@b.c"), request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(bodyOf(response).code()).isEqualTo("RESOURCE_CONFLICT");
        }
    }

    @Nested
    @DisplayName("validation failures")
    class ValidationFailures {

        @Test
        @DisplayName("MethodArgumentNotValidException maps to 400 VALIDATION_ERROR with field errors")
        void methodArgumentNotValid() throws Exception {
            BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "productRequest");
            bindingResult.addError(new org.springframework.validation.FieldError(
                    "productRequest", "price", "must be greater than 0"));
            bindingResult.addError(new org.springframework.validation.FieldError(
                    "productRequest", "name", "must not be blank"));
            MethodParameter methodParameter = new MethodParameter(
                    GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyForParameter", String.class), 0);
            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);

            ResponseEntity<ErrorResponse> response = handler.validation(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(bodyOf(response).code()).isEqualTo("VALIDATION_ERROR");
            assertThat(bodyOf(response).errors())
                    .extracting(ErrorResponse.FieldError::field)
                    .containsExactlyInAnyOrder("price", "name");
        }

        @Test
        @DisplayName("ConstraintViolationException maps to 400 VALIDATION_ERROR with field errors")
        void constraintViolation() {
            ConstraintViolationException ex = new ConstraintViolationException(
                    Set.of(violation("createProduct.price", "must be greater than 0")));

            ResponseEntity<ErrorResponse> response = handler.constraintViolation(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(bodyOf(response).code()).isEqualTo("VALIDATION_ERROR");
            assertThat(bodyOf(response).errors())
                    .singleElement()
                    .satisfies(f -> {
                        assertThat(f.field()).isEqualTo("createProduct.price");
                        assertThat(f.message()).isEqualTo("must be greater than 0");
                    });
        }
    }

    @Nested
    @DisplayName("bad or malformed requests")
    class BadRequests {

        @Test
        @DisplayName("HttpMessageNotReadableException maps to 400 MALFORMED_REQUEST")
        void malformedBody() {
            HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                    "Unexpected end of input", new MockHttpInputMessage(new byte[0]));

            ResponseEntity<ErrorResponse> response = handler.malformedBody(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(bodyOf(response).code()).isEqualTo("MALFORMED_REQUEST");
        }

        @Test
        @DisplayName("IllegalArgumentException maps to 400 BAD_REQUEST carrying its message")
        void illegalArgument() {
            ResponseEntity<ErrorResponse> response =
                    handler.badRequest(new IllegalArgumentException("quantity must be positive"), request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(bodyOf(response).code()).isEqualTo("BAD_REQUEST");
            assertThat(bodyOf(response).message()).isEqualTo("quantity must be positive");
        }

        @Test
        @DisplayName("IllegalArgumentException with no message falls back to 'Invalid request'")
        void illegalArgumentNullMessage() {
            ResponseEntity<ErrorResponse> response =
                    handler.badRequest(new IllegalArgumentException(), request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(bodyOf(response).code()).isEqualTo("BAD_REQUEST");
            assertThat(bodyOf(response).message()).isEqualTo("Invalid request");
        }
    }

    @Nested
    @DisplayName("infrastructure and framework failures")
    class InfrastructureFailures {

        @Test
        @DisplayName("DataIntegrityViolationException maps to 409 RESOURCE_CONFLICT")
        void dataIntegrityViolation() {
            ResponseEntity<ErrorResponse> response =
                    handler.dataIntegrity(new DataIntegrityViolationException("duplicate key"), request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(bodyOf(response).code()).isEqualTo("RESOURCE_CONFLICT");
        }

        @Test
        @DisplayName("NoResourceFoundException maps to 404 ENDPOINT_NOT_FOUND")
        void noResourceFound() {
            NoResourceFoundException ex =
                    new NoResourceFoundException(HttpMethod.GET, "/api/v1/nope", "/api/v1/nope");

            ResponseEntity<ErrorResponse> response = handler.noHandler(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(bodyOf(response).code()).isEqualTo("ENDPOINT_NOT_FOUND");
        }

        @Test
        @DisplayName("generic Exception maps to 500 INTERNAL_ERROR without leaking the message")
        void genericException() {
            ResponseEntity<ErrorResponse> response =
                    handler.generic(new RuntimeException("boom with secrets"), request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(bodyOf(response).code()).isEqualTo("INTERNAL_ERROR");
            assertThat(bodyOf(response).message()).isEqualTo("Internal error");
        }
    }

    @Nested
    @DisplayName("security failures raised inside the dispatcher")
    class SecurityFailures {

        @Test
        @DisplayName("AccessDeniedException maps to 403 ACCESS_DENIED")
        void accessDenied() {
            ResponseEntity<ErrorResponse> response =
                    handler.accessDenied(new AccessDeniedException("nope"), request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(bodyOf(response).code()).isEqualTo("ACCESS_DENIED");
        }

        @Test
        @DisplayName("AuthenticationException maps to 401 UNAUTHORIZED")
        void unauthenticated() {
            ResponseEntity<ErrorResponse> response =
                    handler.unauthenticated(new StubAuthenticationException("bad token"), request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(bodyOf(response).code()).isEqualTo("UNAUTHORIZED");
        }
    }

    // -- helpers -----------------------------------------------------------

    @SuppressWarnings("unused")
    private void dummyForParameter(String argument) {
        // Only its MethodParameter is needed to build a MethodArgumentNotValidException.
    }

    private static ConstraintViolation<?> violation(String propertyPath, String message) {
        return new ConstraintViolation<>() {
            @Override
            public String getMessage() {
                return message;
            }

            @Override
            public String getMessageTemplate() {
                return message;
            }

            @Override
            public Object getRootBean() {
                return null;
            }

            @Override
            public Class<Object> getRootBeanClass() {
                return Object.class;
            }

            @Override
            public Object getLeafBean() {
                return null;
            }

            @Override
            public Object[] getExecutableParameters() {
                return new Object[0];
            }

            @Override
            public Object getExecutableReturnValue() {
                return null;
            }

            @Override
            public Path getPropertyPath() {
                return pathOf(propertyPath);
            }

            @Override
            public Object getInvalidValue() {
                return null;
            }

            @Override
            public jakarta.validation.metadata.ConstraintDescriptor<?> getConstraintDescriptor() {
                return null;
            }

            @Override
            public <U> U unwrap(Class<U> type) {
                return null;
            }
        };
    }

    private static Path pathOf(String value) {
        return new Path() {
            @Override
            public String toString() {
                return value;
            }

            @Override
            public java.util.Iterator<Node> iterator() {
                return java.util.Collections.emptyIterator();
            }
        };
    }

    /** A concrete {@link AuthenticationException}, which is abstract. */
    private static final class StubAuthenticationException extends AuthenticationException {
        StubAuthenticationException(String message) {
            super(message);
        }
    }
}
