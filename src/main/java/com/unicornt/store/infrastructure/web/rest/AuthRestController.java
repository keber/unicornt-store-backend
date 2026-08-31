package com.unicornt.store.infrastructure.web.rest;

import com.unicornt.store.domain.service.UserService;
import com.unicornt.store.infrastructure.persistence.entity.RoleEntity;
import com.unicornt.store.infrastructure.persistence.entity.UserEntity;
import com.unicornt.store.infrastructure.security.JwtService;
import com.unicornt.store.infrastructure.web.dto.AuthDtos.LoginRequest;
import com.unicornt.store.infrastructure.web.dto.AuthDtos.MeResponse;
import com.unicornt.store.infrastructure.web.dto.AuthDtos.RegisterRequest;
import com.unicornt.store.infrastructure.web.dto.AuthDtos.TokenResponse;
import com.unicornt.store.infrastructure.web.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** Registration, login and identity of the caller. */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Account registration and stateless token issuing")
public class AuthRestController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthRestController(UserService userService,
                              AuthenticationManager authenticationManager,
                              JwtService jwtService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new account",
            description = "Creates an account with a BCrypt hashed password and the ROLE_USER role")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created"),
            @ApiResponse(responseCode = "400", description = "Invalid payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email already registered",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MeResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserEntity user = userService.register(
                request.firstName(), request.lastName(), request.email(), request.password());
        return ResponseEntity.created(URI.create("/api/v1/auth/me")).body(toMeResponse(user));
    }

    @PostMapping("/login")
    @Operation(summary = "Exchange credentials for an access token",
            description = "Returns a signed JWT to be sent as an Authorization Bearer header")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token issued"),
            @ApiResponse(responseCode = "400", description = "Invalid payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Bad credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        String token = jwtService.generate(authentication.getName(), roles);
        return ResponseEntity.ok(new TokenResponse(token, jwtService.expirationMs()));
    }

    @GetMapping("/me")
    @Operation(summary = "Describe the authenticated account",
            description = "Resolves the caller from the token in the security context")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MeResponse> me(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            throw new InsufficientAuthenticationException("Authentication required");
        }
        return ResponseEntity.ok(toMeResponse(userService.findByEmail(authentication.getName())));
    }

    private MeResponse toMeResponse(UserEntity user) {
        List<String> roles = user.getRoles().stream()
                .map(RoleEntity::getName)
                .sorted()
                .toList();
        return new MeResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), roles);
    }
}
