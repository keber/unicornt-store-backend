package com.unicornt.store.infrastructure.security;

import tools.jackson.databind.ObjectMapper;
import com.unicornt.store.application.usecase.identity.GetUserByEmailUseCase;
import com.unicornt.store.application.usecase.identity.RegisterUserUseCase;
import com.unicornt.store.domain.model.User;
import com.unicornt.store.infrastructure.web.error.RestAccessDeniedHandler;
import com.unicornt.store.infrastructure.web.error.RestAuthEntryPoint;
import com.unicornt.store.infrastructure.web.rest.AuthRestController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End to end behaviour of the stateless filter chain: token issuing, the JSON shape of the
 * 401 and 403 produced before the dispatcher, and the CORS preflight. The chain is exercised
 * through the real {@link AuthRestController} with the identity use cases mocked out.
 */
@WebMvcTest(controllers = AuthRestController.class)
@ImportAutoConfiguration({SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class})
@Import({SecurityConfig.class, com.unicornt.store.infrastructure.config.CorsConfig.class,
        JwtAuthFilter.class, JwtService.class,
        RestAuthEntryPoint.class, RestAccessDeniedHandler.class,
        SecurityChainTest.AdminOnlyEndpointConfig.class})
class SecurityChainTest {

    private static final String PASSWORD = "s3cret-for-tests";
    private static final String USER_EMAIL = "user@example.com";
    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String FRONTEND_ORIGIN = "http://localhost:5173";

    /** A random key per run, so no signing material is ever written down in the sources. */
    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        byte[] material = new byte[32];
        new SecureRandom().nextBytes(material);
        String encoded = Base64.getEncoder().encodeToString(material);
        registry.add("app.jwt.secret", () -> encoded);
        registry.add("app.jwt.expiration-ms", () -> 3_600_000L);
        registry.add("app.cors.allowed-origins", () -> FRONTEND_ORIGIN);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegisterUserUseCase registerUser;

    @MockitoBean
    private GetUserByEmailUseCase getUserByEmail;

    @MockitoBean
    private UserDetailsService userDetailsService;

    /** Stands in for the catalog write endpoints owned by another task. */
    @TestConfiguration
    static class AdminOnlyEndpointConfig {

        @RestController
        @RequestMapping("/api/v1/products")
        static class ProductStubController {

            @org.springframework.web.bind.annotation.GetMapping
            org.springframework.http.ResponseEntity<String> list() {
                return org.springframework.http.ResponseEntity.ok("[]");
            }

            @PostMapping
            org.springframework.http.ResponseEntity<Void> create() {
                return org.springframework.http.ResponseEntity.created(
                        java.net.URI.create("/api/v1/products/1")).build();
            }
        }
    }

    @Test
    void loginWithValidCredentialsReturnsToken() throws Exception {
        stubAccount(USER_EMAIL, "ROLE_USER");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(3_600_000L));
    }

    @Test
    void loginWithBadCredentialsReturnsUnauthorizedJson() throws Exception {
        when(userDetailsService.loadUserByUsername(anyString()))
                .thenThrow(new UsernameNotFoundException("nope"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("ghost@example.com")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type",
                        org.hamcrest.Matchers.containsString(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void protectedEndpointWithoutTokenReturnsUnauthenticatedErrorResponse() throws Exception {
        mockMvc.perform(post("/api/v1/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type",
                        org.hamcrest.Matchers.containsString(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/v1/products"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void adminEndpointWithUserTokenReturnsAccessDeniedErrorResponse() throws Exception {
        String token = jwtService.generate(USER_EMAIL, List.of("ROLE_USER"));

        mockMvc.perform(post("/api/v1/products").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(header().string("Content-Type",
                        org.hamcrest.Matchers.containsString(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void adminEndpointWithAdminTokenIsAllowed() throws Exception {
        String token = jwtService.generate(ADMIN_EMAIL, List.of("ROLE_ADMIN"));

        mockMvc.perform(post("/api/v1/products").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
    }

    @Test
    void publicCatalogReadNeedsNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk());
    }

    @Test
    void meWithoutTokenReturnsUnauthenticatedErrorResponse() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void meWithTokenDescribesTheAccount() throws Exception {
        when(getUserByEmail.execute(USER_EMAIL)).thenReturn(account(USER_EMAIL, "ROLE_USER"));
        String token = jwtService.generate(USER_EMAIL, List.of("ROLE_USER"));

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(USER_EMAIL))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    void expiredOrMalformedTokenIsTreatedAsAnonymous() throws Exception {
        mockMvc.perform(post("/api/v1/products").header("Authorization", "Bearer not-a-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void registerCreatesTheAccount() throws Exception {
        when(registerUser.execute("Ada", "Lovelace", USER_EMAIL, PASSWORD))
                .thenReturn(account(USER_EMAIL, "ROLE_USER"));

        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "firstName", "Ada", "lastName", "Lovelace",
                "email", USER_EMAIL, "password", PASSWORD));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(USER_EMAIL))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    void preflightFromTheConfiguredOriginReturnsCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/v1/products")
                        .header("Origin", FRONTEND_ORIGIN)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", FRONTEND_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Methods",
                        org.hamcrest.Matchers.containsString("POST")));
    }

    @Test
    void getProductsPreflightFromTheViteOriginIsAllowed() throws Exception {
        mockMvc.perform(options("/api/v1/products")
                        .header("Origin", FRONTEND_ORIGIN)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", FRONTEND_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Methods",
                        org.hamcrest.Matchers.containsString("GET")));
    }

    @Test
    void preflightFromAnUnknownOriginIsRejected() throws Exception {
        mockMvc.perform(options("/api/v1/products")
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    private void stubAccount(String email, String role) {
        when(userDetailsService.loadUserByUsername(email)).thenReturn(
                new org.springframework.security.core.userdetails.User(
                        email,
                        new BCryptPasswordEncoder().encode(PASSWORD),
                        List.of(new SimpleGrantedAuthority(role))));
    }

    private String credentials(String email) throws Exception {
        return objectMapper.writeValueAsString(
                java.util.Map.of("email", email, "password", PASSWORD));
    }

    private User account(String email, String role) {
        return new User(1L, "Ada", "Lovelace", email, "irrelevant-hash", Set.of(role));
    }

    /**
     * The preconditions that make {@code csrf.disable()} in {@link SecurityConfig} safe, pinned
     * so they cannot be undone by accident.
     *
     * <p>A CSRF attack works by making the victim's browser send a request that carries an
     * <em>ambient</em> credential — a cookie, HTTP Basic credentials, a client certificate —
     * attached automatically because of who the browser is, not because the page asked. A bearer
     * token in an {@code Authorization} header is not ambient: script on the attacker's origin
     * has to put it there, and it cannot, because it cannot read the token out of the
     * storefront's {@code localStorage}. A forged {@code POST} from another site therefore
     * arrives unauthenticated and is refused by the {@code anyRequest().authenticated()} default.
     *
     * <p>That reasoning holds only while the API accepts <strong>no</strong> ambient credential.
     * Sonar's {@code java:S4502} flags the disabled CSRF filter because it cannot see that
     * property; these tests assert it. The day someone adds a session, a cookie or Basic auth,
     * disabling CSRF stops being safe — and the build goes red here rather than silently
     * shipping a vulnerability that this comment claims does not exist.
     */
    @org.junit.jupiter.api.Nested
    @org.junit.jupiter.api.DisplayName("the CSRF exemption's preconditions")
    class CsrfExemptionInvariants {

        @Test
        @org.junit.jupiter.api.DisplayName("no response ever sets a cookie")
        void noResponseSetsACookie() throws Exception {
            stubAccount(USER_EMAIL, "ROLE_USER");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(credentials(USER_EMAIL)))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist("Set-Cookie"));

            mockMvc.perform(get("/api/v1/products"))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist("Set-Cookie"));

            mockMvc.perform(post("/api/v1/products"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().doesNotExist("Set-Cookie"));
        }

        @Test
        @org.junit.jupiter.api.DisplayName("no request ever creates a session")
        void noRequestCreatesASession() throws Exception {
            stubAccount(USER_EMAIL, "ROLE_USER");

            var login = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(credentials(USER_EMAIL)))
                    .andExpect(status().isOk())
                    .andReturn();

            org.assertj.core.api.Assertions
                    .assertThat(login.getRequest().getSession(false))
                    .as("a session would be an ambient credential")
                    .isNull();
        }

        @Test
        @org.junit.jupiter.api.DisplayName("a state-changing request without a bearer token is refused")
        void writeWithoutABearerTokenIsRefused() throws Exception {
            mockMvc.perform(post("/api/v1/products"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @org.junit.jupiter.api.DisplayName("cookies grant nothing, including a session cookie")
        void cookiesGrantNothing() throws Exception {
            mockMvc.perform(post("/api/v1/products")
                            .cookie(new jakarta.servlet.http.Cookie("JSESSIONID", "forged-session"),
                                    new jakarta.servlet.http.Cookie("token", jwtService.generate(
                                            USER_EMAIL, List.of("ROLE_ADMIN")))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @org.junit.jupiter.api.DisplayName("HTTP Basic credentials are refused")
        void basicCredentialsAreRefused() throws Exception {
            stubAccount(ADMIN_EMAIL, "ROLE_ADMIN");
            String basic = Base64.getEncoder()
                    .encodeToString((ADMIN_EMAIL + ":" + PASSWORD).getBytes());

            mockMvc.perform(post("/api/v1/products")
                            .header("Authorization", "Basic " + basic))
                    .andExpect(status().isUnauthorized());
        }
    }
}
