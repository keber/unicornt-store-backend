package com.unicornt.store.application.usecase.cart;

import com.unicornt.store.domain.repository.CartRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.unicornt.store.application.usecase.cart.CartUseCaseFixtures.USER;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClearCartUseCase")
class ClearCartUseCaseTest {

    @Mock
    private CartRepository carts;
    @InjectMocks
    private ClearCartUseCase useCase;

    @Test
    @DisplayName("delegates to the repository to empty the caller's cart")
    void clearsCart() {
        useCase.execute(USER);

        verify(carts).deleteByUserId(USER);
    }
}
