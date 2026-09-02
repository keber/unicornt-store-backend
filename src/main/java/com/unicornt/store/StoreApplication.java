package com.unicornt.store;

import com.unicornt.store.infrastructure.persistence.entity.RoleEntity;
import com.unicornt.store.infrastructure.persistence.entity.UserEntity;
import com.unicornt.store.infrastructure.persistence.repository.RoleRepository;
import com.unicornt.store.infrastructure.persistence.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

/**
 * Entry point of the Spring Boot application.
 * Extends SpringBootServletInitializer to allow deployment as a WAR
 * on an external Tomcat (10.1+).
 */
@SpringBootApplication
public class StoreApplication extends SpringBootServletInitializer {

    private static final Logger log = LoggerFactory.getLogger(StoreApplication.class);

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(StoreApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(StoreApplication.class, args);
    }

    /**
     * Creates a single administrator account from operator-supplied configuration.
     * <p>
     * Roles and every other piece of reference data are seeded by the versioned
     * migration {@code V2__seed_reference_data.sql}; this runner only bootstraps
     * the first privileged account, which cannot be created through the public
     * {@code /api/v1/auth/register} endpoint (that endpoint only grants
     * {@code ROLE_USER}).
     * <p>
     * The bean exists only when {@code app.bootstrap-admin.email} is set, so a
     * default deployment creates no account and ships no credential. When the
     * matching {@code app.bootstrap-admin.password} is left blank a strong
     * password is generated and logged once for the operator to record and
     * rotate. The runner is idempotent: it does nothing if the address already
     * exists.
     */
    @Bean
    @ConditionalOnProperty(name = "app.bootstrap-admin.email")
    CommandLineRunner bootstrapAdmin(@Value("${app.bootstrap-admin.email}") String email,
                                     @Value("${app.bootstrap-admin.password:}") String configuredPassword,
                                     RoleRepository roleRepository,
                                     UserRepository userRepository,
                                     PasswordEncoder passwordEncoder) {
        return args -> {
            String address = email.trim();
            if (userRepository.findByEmail(address).isPresent()) {
                log.info("Bootstrap admin {} already exists; nothing to do.", address);
                return;
            }

            RoleEntity adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new IllegalStateException(
                            "ROLE_ADMIN not found. Run the reference-data migration before bootstrapping an admin."));

            boolean generated = !StringUtils.hasText(configuredPassword);
            String rawPassword = generated ? generatePassword() : configuredPassword;

            UserEntity admin = new UserEntity();
            admin.setFirstName("Store");
            admin.setLastName("Admin");
            admin.setEmail(address);
            admin.setPassword(passwordEncoder.encode(rawPassword));
            admin.setRoles(Set.of(adminRole));
            userRepository.save(admin);

            if (generated) {
                log.warn("""
                        =====================================================================
                        Bootstrap admin created.
                          email:    {}
                          password: {}
                        Record this password now and change it after the first login;
                        it is not stored anywhere else and will not be shown again.
                        =====================================================================""",
                        address, rawPassword);
            } else {
                log.info("Bootstrap admin {} created with the configured password.", address);
            }
        };
    }

    private static String generatePassword() {
        byte[] bytes = new byte[24];
        new SecureRandom(1000).nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
