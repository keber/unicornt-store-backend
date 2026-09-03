package com.unicornt.store.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BcryptPasswordHasher")
class BcryptPasswordHasherTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private final BcryptPasswordHasher hasher = new BcryptPasswordHasher(encoder);

    @Test
    @DisplayName("produces a BCrypt hash that verifies against the raw password and is not the raw password")
    void hashesWithBcrypt() {
        String hash = hasher.hash("s3cret-value");

        assertThat(hash).startsWith("$2").isNotEqualTo("s3cret-value");
        assertThat(encoder.matches("s3cret-value", hash)).isTrue();
    }

    @Test
    @DisplayName("salts each call so the same password hashes differently")
    void saltsEachCall() {
        assertThat(hasher.hash("same-password")).isNotEqualTo(hasher.hash("same-password"));
    }
}
