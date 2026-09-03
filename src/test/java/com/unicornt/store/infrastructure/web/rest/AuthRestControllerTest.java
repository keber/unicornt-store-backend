package com.unicornt.store.infrastructure.web.rest;

import com.unicornt.store.application.usecase.identity.GetUserByEmailUseCase;
import com.unicornt.store.application.usecase.identity.RegisterUserUseCase;
import com.unicornt.store.domain.exception.DuplicateResourceException;
import com.unicornt.store.domain.model.User;
import com.unicornt.store.infrastructure.security.JwtService;
import com.unicornt.store.infrastructure.web.error.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test for {@link AuthRestController}, wired standalone (no SecurityConfig) so the thin
 * request/response mapping and its error branches are exercised directly. Each request carries
 * the {@link Authentication} the real filter chain would attach at runtime.
 */
@DisplayName("AuthRestController")
class AuthRestControllerTest {

    @Mock
    private RegisterUserUseCase registerUser;
    @Mock
    private GetUserByEmailUseCase getUserByEmail;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthRestController(
                        registerUser, getUserByEmail, authenticationManager, jwtService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(new LocalValidatorFactoryBean())
                .build();
    }

    private static User account(String email, String... roles) {
        return new User(9L, "Ada", "Lovelace", email, "hash", Set.of(roles));
    }

    @Test
    @DisplayName("register returns 201 with a Location header and the account body")
    void registerReturns201() throws Exception {
        when(registerUser.execute("Ada", "Lovelace", "ada@example.com", "s3cret!"))
                .thenReturn(account("ada@example.com", "ROLE_USER"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ada","lastName":"Lovelace",
                                 "email":"ada@example.com","password":"s3cret!"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/auth/me"))
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    @DisplayName("register rejects an invalid body with 400 and per-field errors")
    void registerRejectsInvalidBody() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"","lastName":"Lovelace",
                                 "email":"not-an-email","password":"x"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    @DisplayName("register surfaces a duplicate email as 409")
    void registerConflict() throws Exception {
        when(registerUser.execute(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new DuplicateResourceException("User", "email", "ada@example.com"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ada","lastName":"Lovelace",
                                 "email":"ada@example.com","password":"s3cret!"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));
    }

    @Test
    @DisplayName("me resolves the authenticated principal to the account")
    void meResolvesPrincipal() throws Exception {
        when(getUserByEmail.execute("ada@example.com")).thenReturn(account("ada@example.com", "ROLE_USER"));
        Authentication principal = new UsernamePasswordAuthenticationToken(
                "ada@example.com", null, AuthorityUtils.createAuthorityList("ROLE_USER"));

        mockMvc.perform(get("/api/v1/auth/me").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    @DisplayName("me rejects an anonymous caller with 401")
    void meRejectsAnonymous() throws Exception {
        Authentication anonymous = new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

        mockMvc.perform(get("/api/v1/auth/me").principal(anonymous))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("login authenticates and returns the signed token with its lifetime")
    void loginIssuesToken() throws Exception {
        when(authenticationManager.authenticate(any())).thenReturn(new UsernamePasswordAuthenticationToken(
                "ada@example.com", null, AuthorityUtils.createAuthorityList("ROLE_USER")));
        when(jwtService.generate(anyString(), any())).thenReturn("issued.jwt.token");
        when(jwtService.expirationMs()).thenReturn(3_600_000L);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ada@example.com","password":"s3cret!"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("issued.jwt.token"))
                .andExpect(jsonPath("$.expiresIn").value(3_600_000L));
    }
}
