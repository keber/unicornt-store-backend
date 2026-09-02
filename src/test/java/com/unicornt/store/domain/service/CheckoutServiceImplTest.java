package com.unicornt.store.domain.service;

import com.unicornt.store.domain.exception.OutOfStockException;
import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.infrastructure.persistence.entity.AddressEntity;
import com.unicornt.store.infrastructure.persistence.entity.CartItemEntity;
import com.unicornt.store.infrastructure.persistence.entity.OrderEntity;
import com.unicornt.store.infrastructure.persistence.entity.OrderItemEntity;
import com.unicornt.store.infrastructure.persistence.entity.ProductJpaEntity;
import com.unicornt.store.infrastructure.persistence.entity.UserEntity;
import com.unicornt.store.infrastructure.persistence.repository.OrderRepository;
import com.unicornt.store.infrastructure.persistence.repository.OrderStockRepository;
import com.unicornt.store.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Unit test of the checkout use cases, with no Spring context. */
@ExtendWith(MockitoExtension.class)
class CheckoutServiceImplTest {

    private static final String EMAIL = "buyer@unicornt.test";
    private static final long USER_ID = 7L;
    private static final long ADDRESS_ID = 42L;

    @Mock
    private CartService cartService;
    @Mock
    private AddressService addressService;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderStockRepository orderStockRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CheckoutServiceImpl checkoutService;

    // ----------------------------------------------------------------
    // Object mothers
    // ----------------------------------------------------------------

    private static UserEntity aUser() {
        UserEntity user = new UserEntity();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        return user;
    }

    private static AddressEntity anAddress() {
        AddressEntity address = new AddressEntity();
        address.setId(ADDRESS_ID);
        address.setUserId(USER_ID);
        address.setStreet("221B Baker Street");
        address.setCity("London");
        address.setRegion("England");
        address.setZipCode("NW1");
        return address;
    }

    private static ProductJpaEntity aProduct(int id, String name, int price) {
        ProductJpaEntity product = new ProductJpaEntity();
        product.setId(id);
        product.setName(name);
        product.setPrice(price);
        return product;
    }

    private static CartItemEntity aCartItem(ProductJpaEntity product, int quantity) {
        CartItemEntity item = new CartItemEntity(USER_ID, product.getId(), quantity);
        item.setProduct(product);
        return item;
    }

    @Nested
    @DisplayName("confirm")
    class Confirm {

        @Test
        @DisplayName("An empty cart raises IllegalArgumentException and never clears the cart")
        void emptyCartThrows() {
            when(cartService.getCartItems(EMAIL)).thenReturn(List.of());

            assertThatThrownBy(() -> checkoutService.confirm(EMAIL, ADDRESS_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("The cart is empty");

            verify(cartService, never()).clearCart(any());
            verifyNoInteractions(orderRepository, orderStockRepository, addressService);
        }

        @Test
        @DisplayName("A line without enough stock raises OutOfStockException and never clears the cart or saves the order")
        void insufficientStockThrows() {
            ProductJpaEntity product = aProduct(3, "Hoodie", 5000);
            when(cartService.getCartItems(EMAIL)).thenReturn(List.of(aCartItem(product, 2)));
            when(addressService.findByUserAndId(EMAIL, ADDRESS_ID)).thenReturn(anAddress());
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(aUser()));
            when(orderStockRepository.decreaseStock(3, 2)).thenReturn(0);

            assertThatThrownBy(() -> checkoutService.confirm(EMAIL, ADDRESS_ID))
                    .isInstanceOf(OutOfStockException.class)
                    .hasMessage("Out of stock for product 3");

            verify(cartService, never()).clearCart(any());
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("The happy path prices every line, persists the order, then clears the cart")
        void happyPathBuildsAndSavesOrder() {
            ProductJpaEntity hoodie = aProduct(3, "Hoodie", 5000);
            ProductJpaEntity mug = aProduct(9, "Mug", 1200);
            when(cartService.getCartItems(EMAIL))
                    .thenReturn(List.of(aCartItem(hoodie, 2), aCartItem(mug, 3)));
            when(addressService.findByUserAndId(EMAIL, ADDRESS_ID)).thenReturn(anAddress());
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(aUser()));
            when(orderStockRepository.decreaseStock(anyInt(), anyInt())).thenReturn(1);
            when(orderRepository.save(any(OrderEntity.class))).thenAnswer(call -> call.getArgument(0));

            OrderEntity result = checkoutService.confirm(EMAIL, ADDRESS_ID);

            ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
            verify(orderRepository).save(captor.capture());
            OrderEntity saved = captor.getValue();

            assertThat(saved).isSameAs(result);
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.getAddressId()).isEqualTo(ADDRESS_ID);
            assertThat(saved.getShippingAddress()).isEqualTo("221B Baker Street, London, England NW1");
            assertThat(saved.getStatus()).isEqualTo(OrderEntity.OrderStatus.CONFIRMED);
            // 2 * 5000 + 3 * 1200 = 13600
            assertThat(saved.getTotal()).isEqualByComparingTo(new BigDecimal("13600"));

            assertThat(saved.getItems())
                    .extracting(OrderItemEntity::getProductId,
                            OrderItemEntity::getProductName,
                            OrderItemEntity::getQuantity)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple(3, "Hoodie", 2),
                            org.assertj.core.groups.Tuple.tuple(9, "Mug", 3));

            OrderItemEntity hoodieLine = saved.getItems().get(0);
            assertThat(hoodieLine.getUnitPrice()).isEqualByComparingTo(new BigDecimal("5000"));
            assertThat(hoodieLine.getSubtotal()).isEqualByComparingTo(new BigDecimal("10000"));
            assertThat(hoodieLine.getOrder()).isSameAs(saved);

            OrderItemEntity mugLine = saved.getItems().get(1);
            assertThat(mugLine.getUnitPrice()).isEqualByComparingTo(new BigDecimal("1200"));
            assertThat(mugLine.getSubtotal()).isEqualByComparingTo(new BigDecimal("3600"));
        }

        @Test
        @DisplayName("The order is saved before the cart is cleared")
        void savesOrderBeforeClearingCart() {
            ProductJpaEntity product = aProduct(3, "Hoodie", 5000);
            when(cartService.getCartItems(EMAIL)).thenReturn(List.of(aCartItem(product, 1)));
            when(addressService.findByUserAndId(EMAIL, ADDRESS_ID)).thenReturn(anAddress());
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(aUser()));
            when(orderStockRepository.decreaseStock(anyInt(), anyInt())).thenReturn(1);
            when(orderRepository.save(any(OrderEntity.class))).thenAnswer(call -> call.getArgument(0));

            checkoutService.confirm(EMAIL, ADDRESS_ID);

            InOrder ordered = inOrder(orderRepository, cartService);
            ordered.verify(orderRepository).save(any(OrderEntity.class));
            ordered.verify(cartService).clearCart(EMAIL);
        }

        @Test
        @DisplayName("An unknown user raises ResourceNotFoundException and never saves the order")
        void unknownUserThrows() {
            ProductJpaEntity product = aProduct(3, "Hoodie", 5000);
            when(cartService.getCartItems(EMAIL)).thenReturn(List.of(aCartItem(product, 1)));
            when(addressService.findByUserAndId(EMAIL, ADDRESS_ID)).thenReturn(anAddress());
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> checkoutService.confirm(EMAIL, ADDRESS_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found: " + EMAIL);

            verify(orderRepository, never()).save(any());
            verify(cartService, never()).clearCart(any());
        }
    }

    @Nested
    @DisplayName("findOrders")
    class FindOrders {

        @Test
        @DisplayName("Delegates to the repository scoped to the resolved user id")
        void delegatesToRepository() {
            OrderEntity order = new OrderEntity();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(aUser()));
            when(orderRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of(order));

            assertThat(checkoutService.findOrders(EMAIL)).containsExactly(order);
        }

        @Test
        @DisplayName("An unknown user raises ResourceNotFoundException")
        void unknownUserThrows() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> checkoutService.findOrders(EMAIL))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found: " + EMAIL);
        }
    }

    @Nested
    @DisplayName("findOrder")
    class FindOrder {

        @Test
        @DisplayName("Returns the order when it belongs to the user")
        void returnsOwnedOrder() {
            OrderEntity order = new OrderEntity();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(aUser()));
            when(orderRepository.findByIdAndUserId(100L, USER_ID)).thenReturn(Optional.of(order));

            assertThat(checkoutService.findOrder(EMAIL, 100L)).isSameAs(order);
        }

        @Test
        @DisplayName("A missing or foreign order raises ResourceNotFoundException")
        void unknownOrderThrows() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(aUser()));
            when(orderRepository.findByIdAndUserId(eq(100L), eq(USER_ID))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> checkoutService.findOrder(EMAIL, 100L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Order not found: 100");
        }

        @Test
        @DisplayName("An unknown user raises ResourceNotFoundException")
        void unknownUserThrows() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> checkoutService.findOrder(EMAIL, 100L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found: " + EMAIL);
        }
    }
}
