package com.unicornt.store.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Request and response payloads of the authentication endpoints. */
public final class AuthDtos {

    private AuthDtos() {
    }

    /** Body of a self-registration request. */
    @Schema(name = "RegisterRequest", description = "Data required to open a new account")
    public record RegisterRequest(

            @Schema(description = "Given name", example = "Ada", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "First name is required")
            @Size(max = 100, message = "First name must not exceed 100 characters")
            String firstName,

            @Schema(description = "Family name", example = "Lovelace", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "Last name is required")
            @Size(max = 100, message = "Last name must not exceed 100 characters")
            String lastName,

            @Schema(description = "Email, also used as the login identity", example = "ada@example.com",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "Email is required")
            @Email(message = "Email must be a well formed address")
            @Size(max = 150, message = "Email must not exceed 150 characters")
            String email,

            @Schema(description = "Plain password, stored hashed with BCrypt", example = "s3cret!",
                    requiredMode = Schema.RequiredMode.REQUIRED, minLength = 6)
            @NotBlank(message = "Password is required")
            @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
            String password) {
    }

    /** Body of a login request. */
    @Schema(name = "LoginRequest", description = "Credentials exchanged for an access token")
    public record LoginRequest(

            @Schema(description = "Registered email", example = "ada@example.com",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "Email is required")
            String email,

            @Schema(description = "Account password", example = "s3cret!",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "Password is required")
            String password) {
    }

    /** Access token issued after a successful login. */
    @Schema(name = "TokenResponse", description = "Signed access token and its lifetime")
    public record TokenResponse(

            @Schema(description = "JWT to be sent as an Authorization Bearer header",
                    example = "eyJhbGciOiJIUzI1NiJ9...")
            String token,

            @Schema(description = "Token lifetime in milliseconds", example = "3600000")
            long expiresIn) {
    }

    /** Identity of the caller, resolved from the security context. */
    @Schema(name = "MeResponse", description = "Account behind the presented token")
    public record MeResponse(

            @Schema(description = "Account identifier", example = "7")
            Long id,

            @Schema(description = "Given name", example = "Ada")
            String firstName,

            @Schema(description = "Family name", example = "Lovelace")
            String lastName,

            @Schema(description = "Registered email", example = "ada@example.com")
            String email,

            @Schema(description = "Granted authorities, prefixed with ROLE_", example = "[\"ROLE_USER\"]")
            List<String> roles) {
    }
}
