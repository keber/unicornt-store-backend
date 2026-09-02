package com.unicornt.store.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Global CORS configuration for the browser frontend, which is served from its own
 * origin (the Vite dev server on {@code http://localhost:5173}).
 *
 * <p>There is no {@code @CrossOrigin} anywhere on the controllers: the single source
 * of truth is the {@link CorsConfigurationSource} bean below, picked up by Spring
 * Security through {@code http.cors(Customizer.withDefaults())} in
 * {@code SecurityConfig}. The allowed origins come from
 * {@code app.cors.allowed-origins} (env var {@code APP_CORS_ALLOWED_ORIGINS}) as a
 * comma-separated list, defaulting to the Vite dev origin.</p>
 */
@Configuration
public class CorsConfig {

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setExposedHeaders(List.of("Location"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
