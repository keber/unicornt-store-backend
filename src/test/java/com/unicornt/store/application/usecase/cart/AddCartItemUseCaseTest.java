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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AddCartItemUseCase")
class AddCartItemUseCaseTest {

    @Mock
    private CartRepository carts;
    @Mock
    private ProductRepository products;
    @InjectMocks
    private AddCartItemUseCase useCase;

    @Test
    @DisplayName("creates a new line and flags it as created")
    void createsNewLine() {
        when(products.findById(10)).thenReturn(Optional.of(product(10, 150, 100)));
        when(carts.findByUserId(USER)).thenReturn(Cart.empty(USER));
        when(carts.save(any(Cart.class))).thenAnswer(call -> call.getArgument(0));

        AddCartItemUseCase.Result result = useCase.execute(USER, 10, 3);

        assertThat(result.created()).isTrue();
        ArgumentCaptor<Cart> saved = ArgumentCaptor.forClass(Cart.class);
        verify(carts).save(saved.capture());
        assertThat(saved.getValue().findItem(10)).contains(CartItem.of(10, 3));
        assertThat(result.cart().items().get(0).subtotal().amount()).isEqualTo(450);
    }

    @Test
    @DisplayName("sums into an existing line and does not flag it as created")
    void mergesQuantity() {
        when(products.findById(10)).thenReturn(Optional.of(product(10, 150, 100)));
        when(carts.findByUserId(USER)).thenReturn(new Cart(USER, List.of(CartItem.of(10, 2))));
        when(carts.save(any(Cart.class))).thenAnswer(call -> call.getArgument(0));

        AddCartItemUseCase.Result result = useCase.execute(USER, 10, 3);

        assertThat(result.created()).isFalse();
        assertThat(result.cart().items().get(0).quantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("a missing product is reported as not found and nothing is saved")
    void missingProduct() {
        when(products.findById(10)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> useCase.execute(USER, 10, 1))
                .withMessageContaining("Product not found: 10");

        verify(carts, never()).save(any());
    }

    @Test
    @DisplayName("a non-positive quantity is rejected and nothing is saved")
    void rejectsNonPositiveQuantity() {
        when(products.findById(10)).thenReturn(Optional.of(product(10, 150, 100)));
        when(carts.findByUserId(USER)).thenReturn(Cart.empty(USER));

        assertThatIllegalArgumentException().isThrownBy(() -> useCase.execute(USER, 10, 0));

        verify(carts, never()).save(any());
    }
}
