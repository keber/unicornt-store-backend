package com.unicornt.store.application.usecase.cart;

import com.unicornt.store.application.usecase.cart.MergeCartUseCase.IncomingItem;
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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MergeCartUseCase")
class MergeCartUseCaseTest {

    @Mock
    private CartRepository carts;
    @Mock
    private ProductRepository products;
    @InjectMocks
    private MergeCartUseCase useCase;

    private Cart captureSaved() {
        ArgumentCaptor<Cart> saved = ArgumentCaptor.forClass(Cart.class);
        org.mockito.Mockito.verify(carts).save(saved.capture());
        return saved.getValue();
    }

    @Test
    @DisplayName("adds the local quantity on top of the server quantity for the same product")
    void sumsServerAndLocal() {
        when(carts.findByUserId(USER)).thenReturn(new Cart(USER, List.of(CartItem.of(10, 2))));
        when(products.findById(10)).thenReturn(Optional.of(product(10, 100, 50)));
        when(carts.save(any(Cart.class))).thenAnswer(call -> call.getArgument(0));

        useCase.execute(USER, List.of(new IncomingItem(10, 3)));

        assertThat(captureSaved().findItem(10)).contains(CartItem.of(10, 5));
    }

    @Test
    @DisplayName("clamps the summed quantity to the product's current stock")
    void clampsToStock() {
        when(carts.findByUserId(USER)).thenReturn(new Cart(USER, List.of(CartItem.of(10, 4))));
        when(products.findById(10)).thenReturn(Optional.of(product(10, 100, 6)));
        when(carts.save(any(Cart.class))).thenAnswer(call -> call.getArgument(0));

        useCase.execute(USER, List.of(new IncomingItem(10, 10)));

        assertThat(captureSaved().findItem(10)).contains(CartItem.of(10, 6));
    }

    @Test
    @DisplayName("drops an incoming line whose product no longer exists")
    void dropsUnknownProduct() {
        when(carts.findByUserId(USER)).thenReturn(Cart.empty(USER));
        when(products.findById(99)).thenReturn(Optional.empty());
        when(carts.save(any(Cart.class))).thenAnswer(call -> call.getArgument(0));

        useCase.execute(USER, List.of(new IncomingItem(99, 2)));

        assertThat(captureSaved().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("drops an incoming line for an out-of-stock product")
    void dropsOutOfStock() {
        when(carts.findByUserId(USER)).thenReturn(Cart.empty(USER));
        when(products.findById(10)).thenReturn(Optional.of(product(10, 100, 0)));
        when(carts.save(any(Cart.class))).thenAnswer(call -> call.getArgument(0));

        useCase.execute(USER, List.of(new IncomingItem(10, 2)));

        assertThat(captureSaved().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("prices the merged cart in the response")
    void pricesResult() {
        when(carts.findByUserId(USER)).thenReturn(Cart.empty(USER));
        when(products.findById(10)).thenReturn(Optional.of(product(10, 100, 50)));
        when(carts.save(any(Cart.class))).thenAnswer(call -> call.getArgument(0));

        PricedCart result = useCase.execute(USER, List.of(new IncomingItem(10, 3)));

        assertThat(result.itemCount()).isEqualTo(3);
        assertThat(result.total().amount()).isEqualTo(300);
    }

    @Test
    @DisplayName("an incoming line rejects a non-positive quantity at construction")
    void incomingItemGuards() {
        assertThatIllegalArgumentException().isThrownBy(() -> new IncomingItem(10, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> new IncomingItem(0, 1));
    }
}
