package com.unicornt.store.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless security for the REST API: no session, no form login, no CSRF token, and every
 * authentication carried by a JWT bearer token.
 *
 * <p><strong>Extending the request matchers.</strong> Authorization lives in two places.
 * Coarse, path shaped rules belong to {@link #chain} below and are grouped into the blocks
 * marked PUBLIC, ADMIN and the anyRequest default; a new rule is added inside its matching
 * block, always keeping the most specific matcher first. Rules that depend on the resource
 * rather than on the path, such as owning a cart or an order, belong on the controller method
 * as a method security annotation and need no change here, because the anyRequest default
 * already requires authentication for them.</p>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /** Endpoints reachable without a token. */
    private static final String[] PUBLIC_PATHS = {
            "/api/v1/auth/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api-docs/**",
            "/v3/api-docs/**"
    };

    /** Read only catalog paths, public for the storefront. */
    private static final String[] PUBLIC_CATALOG_PATHS = {
            "/api/v1/products/**",
            "/api/v1/categories/**"
    };

    /** Catalog paths whose writes are reserved to administrators. */
    private static final String[] ADMIN_CATALOG_PATHS = {
            "/api/v1/products/**",
            "/api/v1/categories/**"
    };

    @Bean
    SecurityFilterChain chain(HttpSecurity http,
                              JwtAuthFilter jwtAuthFilter,
                              AuthenticationEntryPoint authenticationEntryPoint,
                              AccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // AUTHENTICATED, declared before the public auth prefix below
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()
                        // PUBLIC
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_CATALOG_PATHS).permitAll()
                        // ADMIN
                        .requestMatchers(HttpMethod.POST, ADMIN_CATALOG_PATHS).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, ADMIN_CATALOG_PATHS).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, ADMIN_CATALOG_PATHS).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, ADMIN_CATALOG_PATHS).hasRole("ADMIN")
                        // DEFAULT: carts, orders and addresses refine this with method security
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Explicit manager so the login endpoint authenticates against the stored accounts. */
    @Bean
    AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    // CORS is configured globally in infrastructure.config.CorsConfig and consumed
    // here through http.cors(Customizer.withDefaults()). There is no @CrossOrigin
    // on any controller.
}
