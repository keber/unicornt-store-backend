package com.unicornt.store.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @Test
    void exposesTheTwoLifecycleStates() {
        assertThat(OrderStatus.values()).containsExactly(OrderStatus.CONFIRMED, OrderStatus.CANCELLED);
        assertThat(OrderStatus.valueOf("CONFIRMED")).isEqualTo(OrderStatus.CONFIRMED);
    }
}
