package com.unicornt.store.application.usecase.cart;

import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.domain.model.Cart;
import com.unicornt.store.domain.model.CartItem;
import com.unicornt.store.domain.repository.CartRepository;
import com.unicornt.store.domain.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.unicornt.store.application.usecase.cart.CartUseCaseFixtures.USER;
import static com.unicornt.store.application.usecase.cart.CartUseCaseFixtures.product;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateCartItemUseCase")
class UpdateCartItemUseCaseTest {

    @Mock
    private CartRepository carts;
    @Mock
    private ProductRepository products;
    @InjectMocks
    private UpdateCartItemUseCase useCase;

    @Test
    @DisplayName("replaces the quantity of an existing line")
    void replacesQuantity() {
        when(carts.findByUserId(USER)).thenReturn(new Cart(USER, List.of(CartItem.of(10, 2))));
        when(carts.save(any(Cart.class))).thenAnswer(call -> call.getArgument(0));
        when(products.findById(10)).thenReturn(Optional.of(product(10, 150, 100)));

        PricedCart cart = useCase.execute(USER, 10, 4);

        assertThat(cart.items().get(0).quantity()).isEqualTo(4);
        assertThat(cart.items().get(0).subtotal().amount()).isEqualTo(600);
    }

    @Test
    @DisplayName("a quantity of zero removes the line")
    void zeroRemovesLine() {
        when(carts.findByUserId(USER)).thenReturn(
                new Cart(USER, List.of(CartItem.of(10, 2), CartItem.of(20, 1))));
        when(carts.save(any(Cart.class))).thenAnswer(call -> call.getArgument(0));
        when(products.findById(20)).thenReturn(Optional.of(product(20, 200, 100)));

        PricedCart cart = useCase.execute(USER, 10, 0);

        ArgumentCaptor<Cart> saved = ArgumentCaptor.forClass(Cart.class);
        verify(carts).save(saved.capture());
        assertThat(saved.getValue().findItem(10)).isEmpty();
        assertThat(cart.items()).extracting(PricedCart.Line::productId).containsExactly(20L);
    }

    @Test
    @DisplayName("an unknown line is reported as not found and nothing is saved")
    void unknownLine() {
        when(carts.findByUserId(USER)).thenReturn(Cart.empty(USER));

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> useCase.execute(USER, 10, 3))
                .withMessageContaining("Cart item not found: 10");

        verify(carts, never()).save(any());
    }
}
