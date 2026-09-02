package com.unicornt.store.application.usecase.cart;

import com.unicornt.store.domain.model.Cart;
import com.unicornt.store.domain.model.CartItem;
import com.unicornt.store.domain.repository.CartRepository;
import com.unicornt.store.domain.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.unicornt.store.application.usecase.cart.CartUseCaseFixtures.USER;
import static com.unicornt.store.application.usecase.cart.CartUseCaseFixtures.product;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetCartUseCase")
class GetCartUseCaseTest {

    @Mock
    private CartRepository carts;
    @Mock
    private ProductRepository products;
    @InjectMocks
    private GetCartUseCase useCase;

    @Test
    @DisplayName("prices every line and rolls up the item count and total")
    void pricesLines() {
        when(carts.findByUserId(USER)).thenReturn(
                new Cart(USER, List.of(CartItem.of(10, 2), CartItem.of(20, 3))));
        when(products.findById(10)).thenReturn(Optional.of(product(10, 150, 100)));
        when(products.findById(20)).thenReturn(Optional.of(product(20, 200, 100)));

        PricedCart cart = useCase.execute(USER);

        assertThat(cart.itemCount()).isEqualTo(5);
        assertThat(cart.total().amount()).isEqualTo(2 * 150 + 3 * 200);
        assertThat(cart.items()).extracting(PricedCart.Line::productId).containsExactly(10L, 20L);
        assertThat(cart.items().get(0).subtotal().amount()).isEqualTo(300);
    }

    @Test
    @DisplayName("drops a line whose product no longer exists")
    void dropsDeletedProduct() {
        when(carts.findByUserId(USER)).thenReturn(
                new Cart(USER, List.of(CartItem.of(10, 2), CartItem.of(99, 4))));
        when(products.findById(10)).thenReturn(Optional.of(product(10, 150, 100)));
        when(products.findById(99)).thenReturn(Optional.empty());

        PricedCart cart = useCase.execute(USER);

        assertThat(cart.items()).extracting(PricedCart.Line::productId).containsExactly(10L);
        assertThat(cart.itemCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("an empty cart yields a zero item count and zero total")
    void emptyCart() {
        when(carts.findByUserId(USER)).thenReturn(Cart.empty(USER));

        PricedCart cart = useCase.execute(USER);

        assertThat(cart.items()).isEmpty();
        assertThat(cart.itemCount()).isZero();
        assertThat(cart.total().amount()).isZero();
    }
}
