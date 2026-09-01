package com.unicornt.store.infrastructure.web.error;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Renders the 403 raised by a request matcher in the security filter chain using the same
 * ErrorResponse payload the global exception handler produces for a method security denial
 * inside the dispatcher.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    /** Machine readable code of an authenticated but insufficiently privileged request. */
    public static final String CODE = "ACCESS_DENIED";

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(
                "Access denied", CODE, HttpStatus.FORBIDDEN, request.getRequestURI()));
    }
}
