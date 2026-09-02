package com.unicornt.store.application.usecase.identity;

import com.unicornt.store.domain.exception.DuplicateResourceException;
import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.domain.model.User;
import com.unicornt.store.domain.repository.RoleRepository;
import com.unicornt.store.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Behaviour of {@link RegisterUserUseCase}, migrated from the former {@code UserServiceTest}:
 * validate the input, enforce the raw-password length and email-uniqueness rules, require the
 * seeded default role, hash the password and persist the account with {@code ROLE_USER}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterUserUseCase")
class RegisterUserUseCaseTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordHasher passwordHasher;
    @InjectMocks
    private RegisterUserUseCase useCase;

    @Test
    @DisplayName("creates the account with a hashed password and the default role")
    void registersWithDefaultRole() {
        when(userRepository.existsByEmail("juan@test.com")).thenReturn(false);
        when(roleRepository.existsByName(User.ROLE_USER)).thenReturn(true);
        when(passwordHasher.hash("password123")).thenReturn("$2a$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        User result = useCase.execute("Juan", "Perez", "juan@test.com", "password123");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().firstName()).isEqualTo("Juan");
        assertThat(saved.getValue().email()).isEqualTo("juan@test.com");
        assertThat(saved.getValue().passwordHash()).isEqualTo("$2a$encoded");
        assertThat(saved.getValue().roles()).containsExactly(User.ROLE_USER);
        assertThat(result.email()).isEqualTo("juan@test.com");
    }

    @Test
    @DisplayName("trims the email before the uniqueness check and before persisting")
    void normalizesEmail() {
        when(userRepository.existsByEmail("ada@test.com")).thenReturn(false);
        when(roleRepository.existsByName(User.ROLE_USER)).thenReturn(true);
        when(passwordHasher.hash(any())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        User result = useCase.execute("Ada", "Lovelace", "  ada@test.com  ", "abcdef");

        verify(userRepository).existsByEmail("ada@test.com");
        assertThat(result.email()).isEqualTo("ada@test.com");
    }

    @Test
    @DisplayName("rejects an already-registered email with a conflict and never saves")
    void rejectsDuplicateEmail() {
        when(userRepository.existsByEmail("taken@test.com")).thenReturn(true);

        assertThatExceptionOfType(DuplicateResourceException.class)
                .isThrownBy(() -> useCase.execute("Ana", "Lopez", "taken@test.com", "abcdef"));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("fails when the default role has not been seeded")
    void failsWhenDefaultRoleMissing() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(roleRepository.existsByName(User.ROLE_USER)).thenReturn(false);

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> useCase.execute("Ana", "Lopez", "ana@test.com", "abcdef"))
                .withMessageContaining(User.ROLE_USER);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejects a password shorter than six characters and never saves")
    void rejectsShortPassword() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> useCase.execute("Ana", "Lopez", "ana@test.com", "abc"));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejects blank required fields and never saves")
    void rejectsBlankFields() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> useCase.execute("  ", "Lopez", "ana@test.com", "abcdef"));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> useCase.execute("Ana", "Lopez", "  ", "abcdef"));

        verify(userRepository, never()).save(any());
    }
}
