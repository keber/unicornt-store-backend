package com.unicornt.store.application.usecase.ordering;

import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.domain.model.Order;
import com.unicornt.store.domain.model.OrderItem;
import com.unicornt.store.domain.model.ShippingAddress;
import com.unicornt.store.domain.repository.OrderRepository;
import com.unicornt.store.domain.valueobject.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetOrderUseCase / ListOrdersUseCase")
class GetAndListOrdersUseCaseTest {

    @Mock private OrderRepository orders;
    @InjectMocks private GetOrderUseCase getOrder;

    private static Order order() {
        return Order.place("ada@example.com",
                new ShippingAddress("s", "c", "r", null),
                List.of(new OrderItem(1L, "Mug", Money.ofClp(1000), 1)));
    }

    @Test
    @DisplayName("GetOrder returns the caller's order")
    void getReturns() {
        when(orders.findByIdAndUserId(5L, "ada@example.com")).thenReturn(Optional.of(order()));

        assertThat(getOrder.execute(5L, "ada@example.com")).isNotNull();
    }

    @Test
    @DisplayName("GetOrder raises not-found for a missing or foreign order")
    void getMissing() {
        when(orders.findByIdAndUserId(5L, "ada@example.com")).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> getOrder.execute(5L, "ada@example.com"))
                .withMessageContaining("Order not found: 5");
    }

    @Test
    @DisplayName("ListOrders delegates to the repository")
    void listDelegates() {
        ListOrdersUseCase listOrders = new ListOrdersUseCase(orders);
        when(orders.findByUserId("ada@example.com")).thenReturn(List.of(order()));

        assertThat(listOrders.execute("ada@example.com")).hasSize(1);
    }
}
