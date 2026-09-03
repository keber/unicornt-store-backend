package com.unicornt.store.application.usecase.identity;

import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.domain.model.User;
import com.unicornt.store.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetUserByEmailUseCase")
class GetUserByEmailUseCaseTest {

    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private GetUserByEmailUseCase useCase;

    @Test
    @DisplayName("returns the account behind a known email")
    void returnsKnownAccount() {
        User ada = new User(1L, "Ada", "Lovelace", "ada@example.com", "hash", Set.of(User.ROLE_USER));
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(ada));

        assertThat(useCase.execute("ada@example.com")).isSameAs(ada);
    }

    @Test
    @DisplayName("fails with a not-found for an unknown email")
    void failsForUnknownAccount() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> useCase.execute("ghost@example.com"));
    }
}
