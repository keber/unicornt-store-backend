package com.unicornt.store.infrastructure.web.mapper;

import com.unicornt.store.infrastructure.persistence.entity.OrderEntity;
import com.unicornt.store.infrastructure.persistence.entity.OrderItemEntity;
import com.unicornt.store.infrastructure.web.dto.OrderDtos.OrderLineResponse;
import com.unicornt.store.infrastructure.web.dto.OrderDtos.OrderResponse;

import java.util.List;

/** Translation from order entities to order DTOs. Amounts are stored, never recomputed here. */
public final class OrderMapper {

    private OrderMapper() {
    }

    public static OrderLineResponse toResponse(OrderItemEntity item) {
        return new OrderLineResponse(item.getId(), item.getProductId(), item.getProductName(),
                item.getUnitPrice(), item.getQuantity(), item.getSubtotal());
    }

    public static OrderResponse toResponse(OrderEntity order) {
        List<OrderLineResponse> items = order.getItems().stream()
                .map(OrderMapper::toResponse)
                .toList();
        return new OrderResponse(order.getId(), order.getShippingAddress(),
                order.getStatus() != null ? order.getStatus().name() : null,
                order.getTotal(), order.getCreatedAt(), items);
    }
}
