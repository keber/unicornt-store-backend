package com.unicornt.store;

import com.unicornt.store.infrastructure.persistence.entity.RoleEntity;
import com.unicornt.store.infrastructure.persistence.entity.UserEntity;
import com.unicornt.store.infrastructure.persistence.repository.SpringDataRoleRepository;
import com.unicornt.store.infrastructure.persistence.repository.SpringDataUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Behaviour of the {@code bootstrapAdmin} runner on {@link StoreApplication}: it is the only way a
 * {@code ROLE_ADMIN} account comes into existence, since {@code /api/v1/auth/register} grants
 * {@code ROLE_USER} alone. It must be idempotent, it must refuse to run before the reference-data
 * migration has seeded the role, it must never store a raw password, and it must generate a strong
 * one when the operator supplies none.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StoreApplication")
class StoreApplicationTest {

    private static final String EMAIL = "admin@unicornt.local";

    @Mock
    private SpringDataRoleRepository roleRepository;

    @Mock
    private SpringDataUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private final StoreApplication application = new StoreApplication();

    private CommandLineRunner runner(String email, String configuredPassword) {
        return application.bootstrapAdmin(email, configuredPassword, roleRepository, userRepository, passwordEncoder);
    }

    private UserEntity savedUser() {
        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("configure() sources the application class for a WAR deployment")
    void configure_sources_the_application_class() {
        SpringApplicationBuilder builder = new SpringApplicationBuilder();

        assertThat(application.configure(builder)).isSameAs(builder);
    }

    @Nested
    @DisplayName("bootstrapAdmin")
    class BootstrapAdmin {

        @Test
        @DisplayName("does nothing when the address already exists")
        void is_idempotent() throws Exception {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(new UserEntity()));

            runner(EMAIL, "irrelevant").run();

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("refuses to run when ROLE_ADMIN has not been seeded")
        void requires_the_admin_role() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
            when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.empty());

            CommandLineRunner runner = runner(EMAIL, "irrelevant");

            assertThatThrownBy(runner::run)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ROLE_ADMIN");
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("stores the configured password encoded, never in the clear")
        void encodes_the_configured_password() throws Exception {
            RoleEntity adminRole = new RoleEntity("ROLE_ADMIN");
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
            when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
            when(passwordEncoder.encode("s3cret-value")).thenReturn("{bcrypt}hashed");

            runner(EMAIL, "s3cret-value").run();

            UserEntity saved = savedUser();
            assertThat(saved.getEmail()).isEqualTo(EMAIL);
            assertThat(saved.getFirstName()).isEqualTo("Store");
            assertThat(saved.getLastName()).isEqualTo("Admin");
            assertThat(saved.getPassword()).isEqualTo("{bcrypt}hashed");
            assertThat(saved.getRoles()).containsExactly(adminRole);
        }

        @Test
        @DisplayName("trims the configured address before looking it up")
        void trims_the_address() throws Exception {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(new UserEntity()));

            runner("  " + EMAIL + "  ", "irrelevant").run();

            verify(userRepository).findByEmail(EMAIL);
        }

        @Test
        @DisplayName("generates a strong password when none is configured")
        void generates_a_password_when_none_is_configured() throws Exception {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
            when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(new RoleEntity("ROLE_ADMIN")));
            when(passwordEncoder.encode(anyString())).thenReturn("{bcrypt}hashed");

            runner(EMAIL, "   ").run();

            ArgumentCaptor<String> rawPassword = ArgumentCaptor.forClass(String.class);
            verify(passwordEncoder).encode(rawPassword.capture());
            assertThat(rawPassword.getValue())
                    .as("24 random bytes, url-safe base64, unpadded")
                    .hasSize(32)
                    .doesNotContain("=");
            assertThat(savedUser().getPassword()).isEqualTo("{bcrypt}hashed");
        }

        @Test
        @DisplayName("generates a different password every time")
        void generated_passwords_are_not_reused() throws Exception {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
            when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(new RoleEntity("ROLE_ADMIN")));
            when(passwordEncoder.encode(anyString())).thenReturn("{bcrypt}hashed");

            runner(EMAIL, null).run();
            runner(EMAIL, null).run();

            ArgumentCaptor<String> rawPassword = ArgumentCaptor.forClass(String.class);
            verify(passwordEncoder, times(2)).encode(rawPassword.capture());
            assertThat(rawPassword.getAllValues()).doesNotHaveDuplicates();
        }
    }
}
