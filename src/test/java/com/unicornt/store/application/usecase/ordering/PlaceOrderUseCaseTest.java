package com.unicornt.store.application.usecase.ordering;

import com.unicornt.store.application.usecase.cart.ClearCartUseCase;
import com.unicornt.store.application.usecase.cart.GetCartUseCase;
import com.unicornt.store.application.usecase.cart.PricedCart;
import com.unicornt.store.domain.exception.OutOfStockException;
import com.unicornt.store.domain.model.Order;
import com.unicornt.store.domain.model.ShippingAddress;
import com.unicornt.store.domain.repository.OrderRepository;
import com.unicornt.store.domain.repository.StockRepository;
import com.unicornt.store.domain.valueobject.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlaceOrderUseCase")
class PlaceOrderUseCaseTest {

    private static final String USER = "ada@example.com";
    private static final ShippingAddress ADDRESS = new ShippingAddress("Av. 1234", "Santiago", "RM", "7500000");

    @Mock private GetCartUseCase getCart;
    @Mock private ClearCartUseCase clearCart;
    @Mock private StockRepository stock;
    @Mock private OrderRepository orders;
    @InjectMocks private PlaceOrderUseCase useCase;

    private static PricedCart cartWith(PricedCart.Line... lines) {
        int count = 0;
        Money total = Money.ofClp(0);
        for (PricedCart.Line l : lines) {
            count += l.quantity();
            total = total.plus(l.subtotal());
        }
        return new PricedCart(List.of(lines), count, total);
    }

    private static PricedCart.Line line(long productId, String name, int unitPrice, int qty) {
        return new PricedCart.Line(productId, name, name.toLowerCase(),
                Money.ofClp(unitPrice), qty, Money.ofClp(unitPrice * qty));
    }

    @Test
    @DisplayName("decrements stock, snapshots each line, saves the order then clears the cart")
    void happyPath() {
        when(getCart.execute(USER)).thenReturn(cartWith(
                line(1L, "T-shirt", 14990, 1), line(2L, "Mug", 7990, 2)));
        when(stock.decreaseStock(anyLong(), anyInt())).thenReturn(true);
        when(orders.save(any(Order.class))).thenAnswer(call -> {
            Order o = call.getArgument(0);
            return new Order(58L, o.userId(), o.shippingAddress(), o.items(), o.total(), o.status(), o.createdAt());
        });

        OrderConfirmation result = useCase.execute(USER, ADDRESS);

        assertThat(result.id()).isEqualTo(58L);
        assertThat(result.status()).isEqualTo("CONFIRMED");
        assertThat(result.total()).isEqualTo(14990 + 7990 * 2);

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        var order = inOrder(stock, orders, clearCart);
        order.verify(stock).decreaseStock(1L, 1);
        order.verify(stock).decreaseStock(2L, 2);
        order.verify(orders).save(saved.capture());
        order.verify(clearCart).execute(USER);
        assertThat(saved.getValue().items()).extracting("productName").containsExactly("T-shirt", "Mug");
        assertThat(saved.getValue().items().get(0).unitPrice()).isEqualTo(Money.ofClp(14990));
    }

    @Test
    @DisplayName("rejects an empty cart without decrementing stock or saving")
    void emptyCart() {
        when(getCart.execute(USER)).thenReturn(cartWith());

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> useCase.execute(USER, ADDRESS))
                .withMessageContaining("cart is empty");

        verify(stock, never()).decreaseStock(anyLong(), anyInt());
        verify(orders, never()).save(any());
        verify(clearCart, never()).execute(any());
    }

    @Test
    @DisplayName("raises OutOfStock and never saves or clears when a line cannot be reserved")
    void outOfStock() {
        when(getCart.execute(USER)).thenReturn(cartWith(
                line(1L, "T-shirt", 14990, 1), line(2L, "Mug", 7990, 99)));
        when(stock.decreaseStock(1L, 1)).thenReturn(true);
        when(stock.decreaseStock(2L, 99)).thenReturn(false);

        assertThatExceptionOfType(OutOfStockException.class)
                .isThrownBy(() -> useCase.execute(USER, ADDRESS))
                .withMessageContaining("2");

        verify(orders, never()).save(any());
        verify(clearCart, never()).execute(any());
    }
}
