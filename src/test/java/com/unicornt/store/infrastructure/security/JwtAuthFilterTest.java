package com.unicornt.store.infrastructure.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Behaviour of {@link JwtAuthFilter}: it only acts on a well formed {@code Bearer} header, it never
 * overwrites an existing authentication, it swallows a bad token leaving the context empty, and it
 * always lets the request proceed down the chain.
 */
@DisplayName("JwtAuthFilter")
class JwtAuthFilterTest {

    private String secret;
    private JwtService jwtService;
    private JwtAuthFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    private static String randomSecret() {
        byte[] material = new byte[32];
        new SecureRandom().nextBytes(material);
        return Base64.getEncoder().encodeToString(material);
    }

    @BeforeEach
    void setUp() {
        secret = randomSecret();
        jwtService = new JwtService(secret, 3_600_000L);
        filter = new JwtAuthFilter(jwtService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private boolean chainWasInvoked() {
        return chain.getRequest() != null;
    }

    /** A token signed with {@link #secret} but without the roles claim the filter looks for. */
    private String tokenWithoutRolesClaim() {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        return Jwts.builder()
                .subject("ada@example.com")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(key)
                .compact();
    }

    @Nested
    @DisplayName("when there is no usable Authorization header")
    class NoUsableHeader {

        @Test
        @DisplayName("leaves the context anonymous and still calls the chain when the header is absent")
        void missingHeader() throws Exception {
            filter.doFilter(request, response, chain);

            assertThat(currentAuthentication()).isNull();
            assertThat(chainWasInvoked()).isTrue();
        }

        @Test
        @DisplayName("ignores a header that does not start with 'Bearer '")
        void wrongScheme() throws Exception {
            request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

            filter.doFilter(request, response, chain);

            assertThat(currentAuthentication()).isNull();
            assertThat(chainWasInvoked()).isTrue();
        }
    }

    @Nested
    @DisplayName("with a valid Bearer token")
    class ValidToken {

        @Test
        @DisplayName("authenticates with the subject as principal and the roles claim as authorities")
        void populatesAuthentication() throws Exception {
            String token = jwtService.generate("ada@example.com", List.of("ROLE_USER", "ROLE_ADMIN"));
            request.addHeader("Authorization", "Bearer " + token);

            filter.doFilter(request, response, chain);

            Authentication auth = currentAuthentication();
            assertThat(auth).isInstanceOf(UsernamePasswordAuthenticationToken.class);
            assertThat(auth.getPrincipal()).isEqualTo("ada@example.com");
            assertThat(auth.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
            assertThat(auth.getDetails()).isInstanceOf(WebAuthenticationDetails.class);
            assertThat(chainWasInvoked()).isTrue();
        }

        @Test
        @DisplayName("grants no authorities when the token carries no roles claim")
        void noRolesClaim() throws Exception {
            request.addHeader("Authorization", "Bearer " + tokenWithoutRolesClaim());

            filter.doFilter(request, response, chain);

            assertThat(currentAuthentication().getPrincipal()).isEqualTo("ada@example.com");
            assertThat(currentAuthentication().getAuthorities()).isEmpty();
            assertThat(chainWasInvoked()).isTrue();
        }
    }

    @Nested
    @DisplayName("with a token that cannot be trusted")
    class UntrustedToken {

        @Test
        @DisplayName("clears the context and still proceeds when the token is malformed")
        void malformedToken() throws Exception {
            request.addHeader("Authorization", "Bearer not-a-real-token");

            filter.doFilter(request, response, chain);

            assertThat(currentAuthentication()).isNull();
            assertThat(chainWasInvoked()).isTrue();
        }

        @Test
        @DisplayName("clears the context and still proceeds when the token is expired")
        void expiredToken() throws Exception {
            JwtService expiring = new JwtService(randomSecret(), -1_000L);
            JwtAuthFilter filterWithExpiring = new JwtAuthFilter(expiring);
            request.addHeader("Authorization",
                    "Bearer " + expiring.generate("ada@example.com", List.of("ROLE_USER")));

            filterWithExpiring.doFilter(request, response, chain);

            assertThat(currentAuthentication()).isNull();
            assertThat(chainWasInvoked()).isTrue();
        }

        @Test
        @DisplayName("clears the context and still proceeds when the token is signed with another key")
        void foreignSignature() throws Exception {
            JwtService otherIssuer = new JwtService(randomSecret(), 3_600_000L);
            request.addHeader("Authorization",
                    "Bearer " + otherIssuer.generate("ada@example.com", List.of("ROLE_USER")));

            filter.doFilter(request, response, chain);

            assertThat(currentAuthentication()).isNull();
            assertThat(chainWasInvoked()).isTrue();
        }
    }

    @Nested
    @DisplayName("when the context already holds an authentication")
    class PreExistingAuthentication {

        @Test
        @DisplayName("does not overwrite the authentication already in the context")
        void keepsExistingAuthentication() throws Exception {
            Authentication preset = new TestingAuthenticationToken("already@here", null, "ROLE_PRESET");
            SecurityContextHolder.getContext().setAuthentication(preset);
            request.addHeader("Authorization",
                    "Bearer " + jwtService.generate("ada@example.com", List.of("ROLE_USER")));

            filter.doFilter(request, response, chain);

            assertThat(currentAuthentication()).isSameAs(preset);
            assertThat(chainWasInvoked()).isTrue();
        }
    }
}
