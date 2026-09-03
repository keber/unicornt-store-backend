package com.unicornt.store.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/** Fills the last coverage gaps of {@link User}: accessors, hasRole, toString, equals edges. */
@DisplayName("User — accessors and equality edges")
class UserCoverageTest {

    private static User user(Long id, String email) {
        return new User(id, "Ada", "Lovelace", email, "hash", Set.of(User.ROLE_USER, User.ROLE_ADMIN));
    }

    @Test
    @DisplayName("exposes every field")
    void accessors() {
        User u = user(7L, "ada@example.com");

        assertThat(u.id()).isEqualTo(7L);
        assertThat(u.firstName()).isEqualTo("Ada");
        assertThat(u.lastName()).isEqualTo("Lovelace");
        assertThat(u.email()).isEqualTo("ada@example.com");
        assertThat(u.passwordHash()).isEqualTo("hash");
        assertThat(u.roles()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("rejects a null (not merely blank) required field")
    void rejectsNullFields() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new User(null, null, "L", "a@b.cl", "h", Set.of("ROLE_USER")))
                .withMessageContaining("first name");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new User(null, "F", "L", "a@b.cl", null, Set.of("ROLE_USER")))
                .withMessageContaining("password hash");
    }

    @Test
    @DisplayName("hasRole reports membership")
    void hasRole() {
        User u = user(1L, "a@b.cl");

        assertThat(u.hasRole("ROLE_ADMIN")).isTrue();
        assertThat(u.hasRole("ROLE_GHOST")).isFalse();
    }

    @Test
    @DisplayName("equals keys on id and email, and rejects null / other types")
    void equalsEdges() {
        assertThat(user(1L, "a@b.cl"))
                .isEqualTo(user(1L, "a@b.cl"))
                .hasSameHashCodeAs(user(1L, "a@b.cl"))
                .isNotEqualTo(user(2L, "a@b.cl"))
                .isNotEqualTo(user(1L, "other@b.cl"))
                .isNotEqualTo(null)
                .isNotEqualTo("user");
        assertThat(user(null, "a@b.cl")).isEqualTo(user(null, "a@b.cl"));
        assertThat(user(null, "a@b.cl")).isNotEqualTo(user(1L, "a@b.cl"));
    }

    @Test
    @DisplayName("toString shows the id, email and roles")
    void readableToString() {
        assertThat(user(1L, "a@b.cl").toString()).contains("a@b.cl").contains("ROLE_USER");
    }
}
