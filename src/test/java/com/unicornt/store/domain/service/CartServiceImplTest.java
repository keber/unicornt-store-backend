package com.unicornt.store.domain.service;

import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.domain.service.CartService.CartLine;
import com.unicornt.store.domain.service.CartService.CartView;
import com.unicornt.store.infrastructure.persistence.entity.CartItemEntity;
import com.unicornt.store.infrastructure.persistence.entity.ProductJpaEntity;
import com.unicornt.store.infrastructure.persistence.entity.UserEntity;
import com.unicornt.store.infrastructure.persistence.repository.CartItemRepository;
import com.unicornt.store.infrastructure.persistence.repository.SpringDataProductRepository;
import com.unicornt.store.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pure unit test of the shopping-cart business rules, no Spring context. */
@ExtendWith(MockitoExtension.class)
@DisplayName("CartServiceImpl")
class CartServiceImplTest {

    private static final String EMAIL = "buyer@test.com";
    private static final Long USER_ID = 7L;

    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SpringDataProductRepository productRepository;

    private CartServiceImpl cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartServiceImpl(cartItemRepository, userRepository, productRepository);
    }

    // ------------------------------------------------------------------
    // Object mothers
    // ------------------------------------------------------------------

    private static UserEntity aUser(Long id, String email) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail(email);
        return user;
    }

    private static ProductJpaEntity aProduct(int id, String name, int price) {
        ProductJpaEntity product = new ProductJpaEntity();
        product.setId(id);
        product.setName(name);
        product.setPrice(price);
        product.setImageBase("img/" + id);
        return product;
    }

    private static CartItemEntity aCartItem(Long id, Long userId, int productId, int quantity) {
        CartItemEntity item = new CartItemEntity(userId, productId, quantity);
        item.setId(id);
        return item;
    }

    private void userExists() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(aUser(USER_ID, EMAIL)));
    }

    // ==================================================================
    @Nested
    @DisplayName("getCart")
    class GetCart {

        @Test
        @DisplayName("prices every line and rolls up itemCount and total")
        void pricesLinesAndTotals() {
            userExists();
            CartItemEntity apples = aCartItem(1L, USER_ID, 10, 2);
            CartItemEntity pears = aCartItem(2L, USER_ID, 20, 3);
            when(cartItemRepository.findByUserId(USER_ID)).thenReturn(List.of(apples, pears));
            when(productRepository.findById(10)).thenReturn(Optional.of(aProduct(10, "Apples", 150)));
            when(productRepository.findById(20)).thenReturn(Optional.of(aProduct(20, "Pears", 200)));

            CartView view = cartService.getCart(EMAIL);

            assertThat(view.items()).hasSize(2);
            assertThat(view.itemCount()).isEqualTo(5);
            assertThat(view.total()).isEqualByComparingTo(new BigDecimal("900"));

            CartLine first = view.items().get(0);
            assertThat(first.productId()).isEqualTo(10);
            assertThat(first.productName()).isEqualTo("Apples");
            assertThat(first.imageBase()).isEqualTo("img/10");
            assertThat(first.unitPrice()).isEqualByComparingTo(new BigDecimal("150"));
            assertThat(first.quantity()).isEqualTo(2);
            assertThat(first.subtotal()).isEqualByComparingTo(new BigDecimal("300"));
        }

        @Test
        @DisplayName("an empty cart yields zero itemCount and zero total")
        void emptyCart() {
            userExists();
            when(cartItemRepository.findByUserId(USER_ID)).thenReturn(List.of());

            CartView view = cartService.getCart(EMAIL);

            assertThat(view.items()).isEmpty();
            assertThat(view.itemCount()).isZero();
            assertThat(view.total()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("a line whose product was deleted is dropped from the totals")
        void dropsDeletedProduct() {
            userExists();
            CartItemEntity live = aCartItem(1L, USER_ID, 10, 2);
            CartItemEntity gone = aCartItem(2L, USER_ID, 99, 4);
            when(cartItemRepository.findByUserId(USER_ID)).thenReturn(List.of(live, gone));
            when(productRepository.findById(10)).thenReturn(Optional.of(aProduct(10, "Apples", 150)));
            when(productRepository.findById(99)).thenReturn(Optional.empty());

            CartView view = cartService.getCart(EMAIL);

            assertThat(view.items()).hasSize(1);
            assertThat(view.itemCount()).isEqualTo(2);
            assertThat(view.total()).isEqualByComparingTo(new BigDecimal("300"));
        }

        @Test
        @DisplayName("an unknown user is reported as missing")
        void unknownUser() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.getCart(EMAIL))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found: " + EMAIL);
        }
    }

    // ==================================================================
    @Nested
    @DisplayName("getCartItems")
    class GetCartItems {

        @Test
        @DisplayName("resolves the product of every surviving line")
        void resolvesProducts() {
            userExists();
            CartItemEntity item = aCartItem(1L, USER_ID, 10, 2);
            ProductJpaEntity product = aProduct(10, "Apples", 150);
            when(cartItemRepository.findByUserId(USER_ID)).thenReturn(List.of(item));
            when(productRepository.findById(10)).thenReturn(Optional.of(product));

            List<CartItemEntity> items = cartService.getCartItems(EMAIL);

            assertThat(items).hasSize(1);
            assertThat(items.get(0).getProduct()).isSameAs(product);
        }

        @Test
        @DisplayName("drops lines whose product no longer exists")
        void dropsDeletedProducts() {
            userExists();
            CartItemEntity live = aCartItem(1L, USER_ID, 10, 2);
            CartItemEntity gone = aCartItem(2L, USER_ID, 99, 1);
            when(cartItemRepository.findByUserId(USER_ID)).thenReturn(List.of(live, gone));
            when(productRepository.findById(10)).thenReturn(Optional.of(aProduct(10, "Apples", 150)));
            when(productRepository.findById(99)).thenReturn(Optional.empty());

            List<CartItemEntity> items = cartService.getCartItems(EMAIL);

            assertThat(items).extracting(CartItemEntity::getId).containsExactly(1L);
        }

        @Test
        @DisplayName("an unknown user is reported as missing")
        void unknownUser() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.getCartItems(EMAIL))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ==================================================================
    @Nested
    @DisplayName("addItem")
    class AddItem {

        @Test
        @DisplayName("creates a new line when the product is not yet in the cart")
        void createsNewLine() {
            userExists();
            ProductJpaEntity product = aProduct(10, "Apples", 150);
            when(productRepository.findById(10)).thenReturn(Optional.of(product));
            when(cartItemRepository.findByUserIdAndProductId(USER_ID, 10)).thenReturn(Optional.empty());
            when(cartItemRepository.save(any(CartItemEntity.class))).thenAnswer(call -> {
                CartItemEntity saved = call.getArgument(0);
                saved.setId(55L);
                return saved;
            });

            CartLine line = cartService.addItem(EMAIL, 10, 3);

            assertThat(line.id()).isEqualTo(55L);
            assertThat(line.productId()).isEqualTo(10);
            assertThat(line.quantity()).isEqualTo(3);
            assertThat(line.subtotal()).isEqualByComparingTo(new BigDecimal("450"));
        }

        @Test
        @DisplayName("merges the quantity into an existing line")
        void mergesQuantity() {
            userExists();
            ProductJpaEntity product = aProduct(10, "Apples", 150);
            CartItemEntity existing = aCartItem(55L, USER_ID, 10, 2);
            when(productRepository.findById(10)).thenReturn(Optional.of(product));
            when(cartItemRepository.findByUserIdAndProductId(USER_ID, 10)).thenReturn(Optional.of(existing));
            when(cartItemRepository.save(any(CartItemEntity.class))).thenAnswer(call -> call.getArgument(0));

            CartLine line = cartService.addItem(EMAIL, 10, 3);

            assertThat(line.quantity()).isEqualTo(5);
            assertThat(line.subtotal()).isEqualByComparingTo(new BigDecimal("750"));
            assertThat(existing.getQuantity()).isEqualTo(5);
        }

        @Test
        @DisplayName("a non-positive quantity is rejected and nothing is saved")
        void rejectsNonPositiveQuantity() {
            assertThatThrownBy(() -> cartService.addItem(EMAIL, 10, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Quantity must be greater than 0");

            verify(cartItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("a missing product is reported as missing and nothing is saved")
        void missingProduct() {
            userExists();
            when(productRepository.findById(10)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.addItem(EMAIL, 10, 1))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Product not found: 10");

            verify(cartItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("a missing user is reported as missing and nothing is saved")
        void missingUser() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.addItem(EMAIL, 10, 1))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found: " + EMAIL);

            verify(cartItemRepository, never()).save(any());
        }
    }

    // ==================================================================
    @Nested
    @DisplayName("updateItemQuantity")
    class UpdateItemQuantity {

        @Test
        @DisplayName("replaces the quantity of an owned line")
        void replacesQuantity() {
            userExists();
            CartItemEntity item = aCartItem(55L, USER_ID, 10, 2);
            when(cartItemRepository.findById(55L)).thenReturn(Optional.of(item));
            when(cartItemRepository.save(any(CartItemEntity.class))).thenAnswer(call -> call.getArgument(0));
            when(productRepository.findById(10)).thenReturn(Optional.of(aProduct(10, "Apples", 150)));

            CartLine line = cartService.updateItemQuantity(EMAIL, 55L, 4);

            assertThat(line.quantity()).isEqualTo(4);
            assertThat(line.subtotal()).isEqualByComparingTo(new BigDecimal("600"));
            assertThat(item.getQuantity()).isEqualTo(4);
        }

        @Test
        @DisplayName("a non-positive quantity is rejected and nothing is saved")
        void rejectsNonPositiveQuantity() {
            assertThatThrownBy(() -> cartService.updateItemQuantity(EMAIL, 55L, -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Quantity must be greater than 0");

            verify(cartItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("a line owned by another user is reported as missing")
        void lineOfAnotherUser() {
            userExists();
            CartItemEntity foreign = aCartItem(55L, 999L, 10, 2);
            when(cartItemRepository.findById(55L)).thenReturn(Optional.of(foreign));

            assertThatThrownBy(() -> cartService.updateItemQuantity(EMAIL, 55L, 4))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Cart item not found: 55");

            verify(cartItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("an unknown line is reported as missing")
        void unknownLine() {
            userExists();
            when(cartItemRepository.findById(55L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.updateItemQuantity(EMAIL, 55L, 4))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(cartItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("a line whose product was deleted is reported as missing")
        void productDeleted() {
            userExists();
            CartItemEntity item = aCartItem(55L, USER_ID, 10, 2);
            when(cartItemRepository.findById(55L)).thenReturn(Optional.of(item));
            when(cartItemRepository.save(any(CartItemEntity.class))).thenAnswer(call -> call.getArgument(0));
            when(productRepository.findById(10)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.updateItemQuantity(EMAIL, 55L, 4))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Product not found: 10");
        }
    }

    // ==================================================================
    @Nested
    @DisplayName("removeItem")
    class RemoveItem {

        @Test
        @DisplayName("deletes an owned line")
        void deletesOwnedLine() {
            userExists();
            CartItemEntity item = aCartItem(55L, USER_ID, 10, 2);
            when(cartItemRepository.findById(55L)).thenReturn(Optional.of(item));

            cartService.removeItem(EMAIL, 55L);

            verify(cartItemRepository).delete(item);
        }

        @Test
        @DisplayName("a line owned by another user is reported as missing and not deleted")
        void lineOfAnotherUser() {
            userExists();
            CartItemEntity foreign = aCartItem(55L, 999L, 10, 2);
            when(cartItemRepository.findById(55L)).thenReturn(Optional.of(foreign));

            assertThatThrownBy(() -> cartService.removeItem(EMAIL, 55L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Cart item not found: 55");

            verify(cartItemRepository, never()).delete(any(CartItemEntity.class));
        }

        @Test
        @DisplayName("an unknown line is reported as missing")
        void unknownLine() {
            userExists();
            when(cartItemRepository.findById(55L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.removeItem(EMAIL, 55L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(cartItemRepository, never()).delete(any(CartItemEntity.class));
        }
    }

    // ==================================================================
    @Nested
    @DisplayName("clearCart")
    class ClearCart {

        @Test
        @DisplayName("empties the cart of the resolved user")
        void emptiesTheCart() {
            userExists();

            cartService.clearCart(EMAIL);

            verify(cartItemRepository).deleteByUserId(USER_ID);
        }

        @Test
        @DisplayName("an unknown user is reported as missing and nothing is deleted")
        void unknownUser() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.clearCart(EMAIL))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(cartItemRepository, never()).deleteByUserId(any());
        }
    }
}
