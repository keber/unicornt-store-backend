package com.unicornt.store.infrastructure.web.error;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Renders the 401 raised inside the security filter chain, before the dispatcher, using the
 * same ErrorResponse payload the global exception handler produces for failures raised
 * inside a controller.
 */
@Component
public class RestAuthEntryPoint implements AuthenticationEntryPoint {

    /** Machine readable code of an unauthenticated request. */
    public static final String CODE = "UNAUTHENTICATED";

    private final ObjectMapper objectMapper;

    public RestAuthEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authenticationException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(
                "Authentication required", CODE, HttpStatus.UNAUTHORIZED, request.getRequestURI()));
    }
}
