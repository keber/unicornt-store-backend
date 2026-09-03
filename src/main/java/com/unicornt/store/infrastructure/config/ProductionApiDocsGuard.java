package com.unicornt.store.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Production-grade security, dimension 3: the OpenAPI description and Swagger UI are
 * a {@code dev}-only tool. They are disabled by {@code springdoc.*} in
 * {@code application*.yml}; this servlet filter is the belt-and-suspenders so that
 * under the {@code prod} profile every {@code /v3/api-docs} and {@code /swagger-ui}
 * path answers {@code 404} regardless of springdoc's own defaults. Runs before the
 * dispatcher, so springdoc never sees the request.
 */
@Component
@Profile("prod")
public class ProductionApiDocsGuard extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (isApiDocsPath(path)) {
            // setStatus, not sendError: sendError triggers an /error dispatch that the
            // stateless security chain then answers with 401.
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":404,\"error\":\"Not Found\",\"path\":\"" + path + "\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static boolean isApiDocsPath(String path) {
        return path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/api-docs");
    }
}
