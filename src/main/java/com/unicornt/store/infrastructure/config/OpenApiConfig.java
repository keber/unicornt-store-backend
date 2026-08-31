package com.unicornt.store.infrastructure.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Global OpenAPI metadata and the bearer JWT security scheme shared by every endpoint that
 * carries {@code @SecurityRequirement(name = "bearerAuth")}. Exposed only under the {@code dev}
 * profile, see {@code springdoc.*} in {@code application*.yml}.
 */
@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = "JWT obtained from POST /api/v1/auth/login, sent as 'Authorization: Bearer <token>'")
public class OpenApiConfig {

    @Bean
    public OpenAPI unicorntStoreOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Unicornt Store API")
                        .version("v1")
                        .description("REST API for the Unicornt store: product catalog, cart, "
                                + "checkout and JWT-based authentication.")
                        .contact(new Contact()
                                .name("Unicornt Store")
                                .email("support@unicornt.store")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
