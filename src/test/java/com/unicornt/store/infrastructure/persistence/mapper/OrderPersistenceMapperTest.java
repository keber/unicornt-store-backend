package com.unicornt.store.infrastructure.persistence.mapper;

import com.unicornt.store.domain.model.Order;
import com.unicornt.store.domain.model.OrderItem;
import com.unicornt.store.domain.model.ShippingAddress;
import com.unicornt.store.domain.valueobject.Money;
import com.unicornt.store.infrastructure.persistence.entity.OrderJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderPersistenceMapper")
class OrderPersistenceMapperTest {

    private static Order domainOrder() {
        return Order.place("ada@example.com",
                new ShippingAddress("Av. 1234", "Santiago", "RM", "7500000"),
                List.of(new OrderItem(2L, "Rainbow Mug", Money.ofClp(7990), 3)));
    }

    @Test
    @DisplayName("toEntity writes the inline address, the enum as text and BigDecimal amounts")
    void toEntity() {
        OrderJpaEntity entity = OrderPersistenceMapper.toEntity(domainOrder(), 42L);

        assertThat(entity.getUserId()).isEqualTo(42L);
        assertThat(entity.getShipStreet()).isEqualTo("Av. 1234");
        assertThat(entity.getShipZip()).isEqualTo("7500000");
        assertThat(entity.getStatus()).isEqualTo(OrderJpaEntity.Status.CONFIRMED);
        assertThat(entity.getTotal()).isEqualByComparingTo(BigDecimal.valueOf(23970));
        assertThat(entity.getItems()).singleElement().satisfies(line -> {
            assertThat(line.getProductName()).isEqualTo("Rainbow Mug");
            assertThat(line.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(7990));
            assertThat(line.getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(23970));
            assertThat(line.getOrder()).isNotNull();
        });
    }

    @Test
    @DisplayName("toDomain rebuilds the order with the supplied principal and exact integer amounts")
    void toDomain() {
        OrderJpaEntity entity = OrderPersistenceMapper.toEntity(domainOrder(), 42L);
        entity.setId(58L);
        entity.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));

        Order roundTripped = OrderPersistenceMapper.toDomain(entity, "ada@example.com");

        assertThat(roundTripped.id()).isEqualTo(58L);
        assertThat(roundTripped.userId()).isEqualTo("ada@example.com");
        assertThat(roundTripped.shippingAddress()).isEqualTo(new ShippingAddress("Av. 1234", "Santiago", "RM", "7500000"));
        assertThat(roundTripped.total()).isEqualTo(Money.ofClp(23970));
        assertThat(roundTripped.items()).singleElement()
                .isEqualTo(new OrderItem(2L, "Rainbow Mug", Money.ofClp(7990), 3));
    }
}
