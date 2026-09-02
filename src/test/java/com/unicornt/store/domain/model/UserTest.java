package com.unicornt.store.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Invariants of the {@link User} model: required fields with a length ceiling, a
 * non-blank stored password hash, at least one non-blank role, and an immutable
 * role set. The raw-password length rule lives in the use case, not here.
 */
@DisplayName("User")
class UserTest {

    private static Set<String> userRole() {
        return Set.of(User.ROLE_USER);
    }

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("keeps the trimmed fields and the given roles")
        void happyPath() {
            User user = User.register("  Ada ", " Lovelace ", " ada@example.com ", "hashed", userRole());

            assertThat(user.id()).isNull();
            assertThat(user.firstName()).isEqualTo("Ada");
            assertThat(user.lastName()).isEqualTo("Lovelace");
            assertThat(user.email()).isEqualTo("ada@example.com");
            assertThat(user.passwordHash()).isEqualTo("hashed");
            assertThat(user.roles()).containsExactly(User.ROLE_USER);
            assertThat(user.hasRole(User.ROLE_USER)).isTrue();
            assertThat(user.hasRole(User.ROLE_ADMIN)).isFalse();
        }

        @Test
        @DisplayName("rejects a blank first name, last name or email")
        void rejectsBlankRequiredFields() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> User.register(" ", "Lovelace", "ada@example.com", "h", userRole()));
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> User.register("Ada", "", "ada@example.com", "h", userRole()));
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> User.register("Ada", "Lovelace", null, "h", userRole()));
        }

        @Test
        @DisplayName("rejects a first or last name longer than 100 characters")
        void rejectsOverlongNames() {
            String tooLong = "x".repeat(101);
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> User.register(tooLong, "Lovelace", "ada@example.com", "h", userRole()));
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> User.register("Ada", tooLong, "ada@example.com", "h", userRole()));
        }

        @Test
        @DisplayName("rejects an email longer than 150 characters")
        void rejectsOverlongEmail() {
            String email = "a".repeat(140) + "@example.com";
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> User.register("Ada", "Lovelace", email, "h", userRole()));
        }

        @Test
        @DisplayName("rejects a blank password hash")
        void rejectsBlankHash() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> User.register("Ada", "Lovelace", "ada@example.com", "  ", userRole()));
        }

        @Test
        @DisplayName("rejects a null or empty role set and a blank role name")
        void rejectsBadRoles() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> User.register("Ada", "Lovelace", "ada@example.com", "h", Set.of()));
            Set<String> withBlank = new LinkedHashSet<>();
            withBlank.add(" ");
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> User.register("Ada", "Lovelace", "ada@example.com", "h", withBlank));
        }

        @Test
        @DisplayName("accepts a persisted account with an id and several roles")
        void persistedAccount() {
            assertThatNoException().isThrownBy(() -> new User(
                    7L, "Ada", "Lovelace", "ada@example.com", "hashed",
                    Set.of(User.ROLE_USER, User.ROLE_ADMIN)));
        }
    }

    @Test
    @DisplayName("exposes an unmodifiable role set")
    void rolesAreUnmodifiable() {
        User user = User.register("Ada", "Lovelace", "ada@example.com", "hashed", userRole());
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> user.roles().add(User.ROLE_ADMIN));
    }

    @Test
    @DisplayName("equals and hashCode key on id and email")
    void equality() {
        User a = new User(1L, "Ada", "Lovelace", "ada@example.com", "h", userRole());
        User sameKey = new User(1L, "Different", "Name", "ada@example.com", "other", userRole());
        User otherEmail = new User(1L, "Ada", "Lovelace", "grace@example.com", "h", userRole());

        assertThat(a).isEqualTo(sameKey).hasSameHashCodeAs(sameKey);
        assertThat(a).isNotEqualTo(otherEmail);
    }
}
