package com.unicornt.store.infrastructure.web.mapper;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.unicornt.store.infrastructure.persistence.entity.OrderEntity;
import com.unicornt.store.infrastructure.persistence.entity.OrderEntity.OrderStatus;
import com.unicornt.store.infrastructure.persistence.entity.OrderItemEntity;
import com.unicornt.store.infrastructure.web.dto.OrderDtos.OrderLineResponse;
import com.unicornt.store.infrastructure.web.dto.OrderDtos.OrderResponse;

/** Unit test of the pure static translation from order entities to order DTOs. */
class OrderMapperTest {

    private static OrderItemEntity anItem() {
        OrderItemEntity item = new OrderItemEntity(
                12, "Unicorn plush", new BigDecimal("14990"), 2, new BigDecimal("29980"));
        item.setId(101L);
        return item;
    }

    private static OrderEntity anOrder() {
        OrderEntity order = new OrderEntity();
        order.setId(58L);
        order.setShippingAddress("Av. Providencia 1234, Santiago");
        order.setStatus(OrderStatus.CONFIRMED);
        order.setTotal(new BigDecimal("29980"));
        order.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        order.setItems(new java.util.ArrayList<>());
        return order;
    }

    @Nested
    @DisplayName("toResponse(OrderItemEntity)")
    class LineResponse {

        @Test
        @DisplayName("copies every stored amount onto the line record without recomputing it")
        void mapsLine() {
            OrderLineResponse line = OrderMapper.toResponse(anItem());

            assertThat(line.id()).isEqualTo(101L);
            assertThat(line.productId()).isEqualTo(12);
            assertThat(line.productName()).isEqualTo("Unicorn plush");
            assertThat(line.unitPrice()).isEqualByComparingTo("14990");
            assertThat(line.quantity()).isEqualTo(2);
            assertThat(line.subtotal()).isEqualByComparingTo("29980");
        }
    }

    @Nested
    @DisplayName("toResponse(OrderEntity)")
    class OrderResponseMapping {

        @Test
        @DisplayName("maps the header fields and the status enum to its name")
        void mapsHeaderAndStatusName() {
            OrderResponse response = OrderMapper.toResponse(anOrder());

            assertThat(response.id()).isEqualTo(58L);
            assertThat(response.shippingAddress()).isEqualTo("Av. Providencia 1234, Santiago");
            assertThat(response.status()).isEqualTo("CONFIRMED");
            assertThat(response.total()).isEqualByComparingTo("29980");
            assertThat(response.createdAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        }

        @Test
        @DisplayName("maps a null status to a null status string")
        void nullStatus() {
            OrderEntity order = anOrder();
            order.setStatus(null);

            assertThat(OrderMapper.toResponse(order).status()).isNull();
        }

        @Test
        @DisplayName("maps the CANCELLED status to its name")
        void cancelledStatus() {
            OrderEntity order = anOrder();
            order.setStatus(OrderStatus.CANCELLED);

            assertThat(OrderMapper.toResponse(order).status()).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("returns an empty item list when the order has no lines")
        void emptyItems() {
            OrderResponse response = OrderMapper.toResponse(anOrder());

            assertThat(response.items()).isEmpty();
        }

        @Test
        @DisplayName("maps every order line in order")
        void mapsEveryLine() {
            OrderEntity order = anOrder();
            order.addItem(anItem());
            OrderItemEntity second = new OrderItemEntity(
                    99, "Sticker", new BigDecimal("990"), 1, new BigDecimal("990"));
            second.setId(102L);
            order.addItem(second);

            OrderResponse response = OrderMapper.toResponse(order);

            assertThat(response.items()).hasSize(2);
            assertThat(response.items()).extracting(OrderLineResponse::id)
                    .containsExactly(101L, 102L);
            assertThat(response.items()).extracting(OrderLineResponse::productName)
                    .containsExactly("Unicorn plush", "Sticker");
        }
    }

    @Nested
    @DisplayName("List helper compatibility")
    class ListUsage {

        @Test
        @DisplayName("the mapped item list is decoupled from later changes to the entity")
        void mappedListIsFresh() {
            OrderEntity order = anOrder();
            order.addItem(anItem());

            OrderResponse response = OrderMapper.toResponse(order);
            order.addItem(new OrderItemEntity(
                    99, "Sticker", new BigDecimal("990"), 1, new BigDecimal("990")));

            assertThat(response.items()).hasSize(1);
        }
    }
}
