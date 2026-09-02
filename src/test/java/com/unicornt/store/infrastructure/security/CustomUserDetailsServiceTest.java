package com.unicornt.store.infrastructure.security;

import com.unicornt.store.domain.model.User;
import com.unicornt.store.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Behaviour of {@link CustomUserDetailsService}: it looks an account up through the
 * {@link UserRepository} domain port, rejects an unknown one, and maps the account roles
 * onto Spring Security authorities while propagating the password hash verbatim.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    private CustomUserDetailsService service;

    @BeforeEach
    void setUp() {
        service = new CustomUserDetailsService(userRepository);
    }

    private static User aUser(String email, String passwordHash, String... roleNames) {
        Set<String> roles = new LinkedHashSet<>(Set.of(roleNames));
        if (roles.isEmpty()) {
            roles.add(User.ROLE_USER);
        }
        return new User(1L, "Ada", "Lovelace", email, passwordHash, roles);
    }

    @Nested
    @DisplayName("when the email is unknown")
    class UnknownEmail {

        @Test
        @DisplayName("throws UsernameNotFoundException naming the missing email")
        void throwsUsernameNotFound() {
            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.loadUserByUsername("ghost@example.com"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found: ghost@example.com");
        }
    }

    @Nested
    @DisplayName("when the account exists")
    class KnownEmail {

        @Test
        @DisplayName("uses the email as the username and propagates the stored password hash")
        void propagatesCredentials() {
            when(userRepository.findByEmail("ada@example.com"))
                    .thenReturn(Optional.of(aUser("ada@example.com", "hashed-secret", "ROLE_USER")));

            UserDetails details = service.loadUserByUsername("ada@example.com");

            assertThat(details.getUsername()).isEqualTo("ada@example.com");
            assertThat(details.getPassword()).isEqualTo("hashed-secret");
        }

        @Test
        @DisplayName("maps every role to a SimpleGrantedAuthority with the role name")
        void mapsRolesToAuthorities() {
            when(userRepository.findByEmail("ada@example.com"))
                    .thenReturn(Optional.of(aUser("ada@example.com", "hashed-secret", "ROLE_USER", "ROLE_ADMIN")));

            UserDetails details = service.loadUserByUsername("ada@example.com");

            assertThat(details.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        }
    }
}
