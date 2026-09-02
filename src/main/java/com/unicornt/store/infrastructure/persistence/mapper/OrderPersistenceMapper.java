package com.unicornt.store.infrastructure.persistence.mapper;

import com.unicornt.store.domain.model.Order;
import com.unicornt.store.domain.model.OrderItem;
import com.unicornt.store.domain.model.OrderStatus;
import com.unicornt.store.domain.model.ShippingAddress;
import com.unicornt.store.domain.valueobject.Money;
import com.unicornt.store.infrastructure.persistence.entity.OrderItemJpaEntity;
import com.unicornt.store.infrastructure.persistence.entity.OrderJpaEntity;

import java.math.BigDecimal;
import java.util.List;

/**
 * Converts between the {@link OrderJpaEntity} row graph and the {@link Order} domain
 * model. CLP amounts are whole units on the domain side and {@code NUMERIC(12,2)} on
 * the row, so they cross as exact integers.
 */
public final class OrderPersistenceMapper {

    private OrderPersistenceMapper() {
    }

    /**
     * @param userId the principal the row was queried with; the row stores a numeric
     *               user id, so the domain identity is supplied by the adapter.
     */
    public static Order toDomain(OrderJpaEntity entity, String userId) {
        List<OrderItem> items = entity.getItems().stream()
                .map(line -> new OrderItem(
                        line.getProductId(),
                        line.getProductName(),
                        Money.ofClp(line.getUnitPrice().intValueExact()),
                        line.getQuantity()))
                .toList();
        return new Order(
                entity.getId(),
                userId,
                new ShippingAddress(entity.getShipStreet(), entity.getShipCity(),
                        entity.getShipRegion(), entity.getShipZip()),
                items,
                Money.ofClp(entity.getTotal().intValueExact()),
                OrderStatus.valueOf(entity.getStatus().name()),
                entity.getCreatedAt());
    }

    /** Builds a fresh row graph for {@code order}; {@code numericUserId} is the resolved id. */
    public static OrderJpaEntity toEntity(Order order, long numericUserId) {
        OrderJpaEntity entity = new OrderJpaEntity();
        entity.setUserId(numericUserId);
        ShippingAddress address = order.shippingAddress();
        entity.setShipStreet(address.street());
        entity.setShipCity(address.city());
        entity.setShipRegion(address.region());
        entity.setShipZip(address.zipCode());
        entity.setTotal(BigDecimal.valueOf(order.total().amount()));
        entity.setStatus(OrderJpaEntity.Status.valueOf(order.status().name()));
        entity.setCreatedAt(order.createdAt());
        for (OrderItem item : order.items()) {
            entity.addItem(new OrderItemJpaEntity(
                    (int) item.productId(),
                    item.productName(),
                    BigDecimal.valueOf(item.unitPrice().amount()),
                    item.quantity(),
                    BigDecimal.valueOf(item.subtotal().amount())));
        }
        return entity;
    }
}
