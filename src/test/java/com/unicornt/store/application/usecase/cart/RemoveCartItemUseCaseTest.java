package com.unicornt.store.application.usecase.cart;

import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.domain.model.Cart;
import com.unicornt.store.domain.model.CartItem;
import com.unicornt.store.domain.repository.CartRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.unicornt.store.application.usecase.cart.CartUseCaseFixtures.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RemoveCartItemUseCase")
class RemoveCartItemUseCaseTest {

    @Mock
    private CartRepository carts;
    @InjectMocks
    private RemoveCartItemUseCase useCase;

    @Test
    @DisplayName("removes an existing line and saves the cart without it")
    void removesLine() {
        when(carts.findByUserId(USER)).thenReturn(
                new Cart(USER, List.of(CartItem.of(10, 2), CartItem.of(20, 1))));
        when(carts.save(any(Cart.class))).thenAnswer(call -> call.getArgument(0));

        useCase.execute(USER, 10);

        ArgumentCaptor<Cart> saved = ArgumentCaptor.forClass(Cart.class);
        verify(carts).save(saved.capture());
        assertThat(saved.getValue().findItem(10)).isEmpty();
        assertThat(saved.getValue().findItem(20)).isPresent();
    }

    @Test
    @DisplayName("an unknown line is reported as not found and nothing is saved")
    void unknownLine() {
        when(carts.findByUserId(USER)).thenReturn(Cart.empty(USER));

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> useCase.execute(USER, 10))
                .withMessageContaining("Cart item not found: 10");

        verify(carts, never()).save(any());
    }
}
