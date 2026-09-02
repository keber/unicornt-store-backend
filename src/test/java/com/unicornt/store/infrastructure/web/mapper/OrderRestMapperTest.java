package com.unicornt.store.infrastructure.web.mapper;

import com.unicornt.store.application.usecase.ordering.OrderConfirmation;
import com.unicornt.store.domain.model.Order;
import com.unicornt.store.domain.model.OrderItem;
import com.unicornt.store.domain.model.OrderStatus;
import com.unicornt.store.domain.model.ShippingAddress;
import com.unicornt.store.domain.valueobject.Money;
import com.unicornt.store.infrastructure.web.dto.OrderDtos.ShippingAddressRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderRestMapper")
class OrderRestMapperTest {

    @Test
    @DisplayName("maps the address request to the domain value object")
    void toDomainAddress() {
        ShippingAddress address = OrderRestMapper.toDomain(
                new ShippingAddressRequest("Av. 1234", "Santiago", "RM", "7500000"));

        assertThat(address).isEqualTo(new ShippingAddress("Av. 1234", "Santiago", "RM", "7500000"));
    }

    @Test
    @DisplayName("maps the confirmation to its response")
    void toConfirmationResponse() {
        assertThat(OrderRestMapper.toResponse(new OrderConfirmation(58L, "CONFIRMED", 23970)))
                .satisfies(r -> {
                    assertThat(r.id()).isEqualTo(58L);
                    assertThat(r.status()).isEqualTo("CONFIRMED");
                    assertThat(r.total()).isEqualTo(23970);
                });
    }

    @Test
    @DisplayName("maps a full order, flattening amounts to whole CLP integers")
    void toOrderResponse() {
        Order order = new Order(58L, "ada@example.com",
                new ShippingAddress("Av. 1234", "Santiago", "RM", null),
                List.of(new OrderItem(2L, "Rainbow Mug", Money.ofClp(7990), 3)),
                Money.ofClp(23970), OrderStatus.CONFIRMED, java.time.Instant.now());

        var response = OrderRestMapper.toResponse(order);

        assertThat(response.id()).isEqualTo(58L);
        assertThat(response.status()).isEqualTo("CONFIRMED");
        assertThat(response.total()).isEqualTo(23970);
        assertThat(response.shippingAddress().region()).isEqualTo("RM");
        assertThat(response.shippingAddress().zipCode()).isNull();
        assertThat(response.items()).singleElement().satisfies(line -> {
            assertThat(line.productId()).isEqualTo(2L);
            assertThat(line.unitPrice()).isEqualTo(7990);
            assertThat(line.subtotal()).isEqualTo(23970);
        });
    }
}
