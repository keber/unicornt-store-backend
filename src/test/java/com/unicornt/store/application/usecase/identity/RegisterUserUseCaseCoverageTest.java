package com.unicornt.store.application.usecase.identity;

import com.unicornt.store.domain.repository.RoleRepository;
import com.unicornt.store.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/** Closes the last branch of {@link RegisterUserUseCase}: a null (not just blank) required field. */
@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterUserUseCase — null required fields")
class RegisterUserUseCaseCoverageTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordHasher passwordHasher;
    @InjectMocks private RegisterUserUseCase useCase;

    @Test
    @DisplayName("rejects a null first name, last name, email or password")
    void rejectsNulls() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> useCase.execute(null, "L", "a@b.cl", "secret1"));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> useCase.execute("F", null, "a@b.cl", "secret1"));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> useCase.execute("F", "L", null, "secret1"));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> useCase.execute("F", "L", "a@b.cl", null));
    }
}
