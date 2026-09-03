package com.unicornt.store.domain.model;

import com.unicornt.store.domain.valueobject.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@DisplayName("Order aggregate")
class OrderTest {

    private static final ShippingAddress ADDRESS = new ShippingAddress("Av. 1234", "Santiago", "RM", "7500000");

    private static List<OrderItem> twoLines() {
        return List.of(
                new OrderItem(1L, "T-shirt", Money.ofClp(14990), 1),
                new OrderItem(2L, "Mug", Money.ofClp(7990), 2));
    }

    @Test
    @DisplayName("place() sums the line subtotals into the total and is born CONFIRMED")
    void placeComputesTotal() {
        Order order = Order.place("ada@example.com", ADDRESS, twoLines());

        assertThat(order.total()).isEqualTo(Money.ofClp(14990 + 7990 * 2));
        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.id()).isNull();
        assertThat(order.createdAt()).isBeforeOrEqualTo(Instant.now());
        assertThat(order.userId()).isEqualTo("ada@example.com");
        assertThat(order.items()).hasSize(2);
    }

    @Test
    @DisplayName("the item list is an immutable copy")
    void itemsAreCopied() {
        Order order = Order.place("ada@example.com", ADDRESS, twoLines());
        List<OrderItem> items = order.items();
        OrderItem extra = new OrderItem(9L, "x", Money.ofClp(1), 1);

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> items.add(extra));
    }

    @Test
    @DisplayName("rejects a blank user, a null address and an empty line list")
    void rejectsInvalid() {
        List<OrderItem> lines = twoLines();
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Order.place("  ", ADDRESS, lines)).withMessageContaining("belong to a user");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Order.place(null, ADDRESS, lines)).withMessageContaining("belong to a user");
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> Order.place("ada", null, lines)).withMessageContaining("shippingAddress");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Order.place("ada", ADDRESS, List.of())).withMessageContaining("at least one line");
    }

    @Test
    @DisplayName("the reconstitution constructor accepts a stored order and rejects nulls")
    void reconstitution() {
        Instant now = Instant.now();
        List<OrderItem> lines = twoLines();
        Money money = Money.ofClp(1);
        Order order = new Order(58L, "ada", ADDRESS, lines, Money.ofClp(30970), OrderStatus.CONFIRMED, now);

        assertThat(order.id()).isEqualTo(58L);
        assertThat(order.createdAt()).isEqualTo(now);

        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> new Order(1L, "ada", ADDRESS, lines, null, OrderStatus.CONFIRMED, now))
                .withMessageContaining("total");
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> new Order(1L, "ada", ADDRESS, lines, money, null, now))
                .withMessageContaining("status");
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> new Order(1L, "ada", ADDRESS, lines, money, OrderStatus.CONFIRMED, null))
                .withMessageContaining("createdAt");
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> new Order(1L, "ada", ADDRESS, null, money, OrderStatus.CONFIRMED, now))
                .withMessageContaining("items");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Order(1L, "ada", ADDRESS, List.of(), money, OrderStatus.CONFIRMED, now))
                .withMessageContaining("at least one line");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Order(1L, "  ", ADDRESS, lines, money, OrderStatus.CONFIRMED, now))
                .withMessageContaining("belong to a user");
    }
}
