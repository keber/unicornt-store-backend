package com.unicornt.store.domain.model;

import com.unicornt.store.domain.valueobject.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@DisplayName("OrderItem")
class OrderItemTest {

    @Test
    @DisplayName("computes the subtotal from the frozen unit price and the quantity")
    void subtotal() {
        OrderItem item = new OrderItem(2L, "Rainbow Mug", Money.ofClp(7990), 3);

        assertThat(item.subtotal()).isEqualTo(Money.ofClp(23970));
        assertThat(item.productName()).isEqualTo("Rainbow Mug");
    }

    @Test
    @DisplayName("trims the product name")
    void trimsName() {
        assertThat(new OrderItem(1L, "  Mug  ", Money.ofClp(1), 1).productName()).isEqualTo("Mug");
    }

    @Test
    @DisplayName("rejects invalid fields")
    void rejectsInvalid() {
        Money price = Money.ofClp(1);
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new OrderItem(0L, "n", price, 1)).withMessageContaining("productId");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new OrderItem(1L, " ", price, 1)).withMessageContaining("productName");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new OrderItem(1L, null, price, 1)).withMessageContaining("productName");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new OrderItem(-3L, "n", price, 1)).withMessageContaining("productId");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new OrderItem(1L, "n", null, 1)).withMessageContaining("unitPrice");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new OrderItem(1L, "n", price, 0)).withMessageContaining("quantity");
    }
}
