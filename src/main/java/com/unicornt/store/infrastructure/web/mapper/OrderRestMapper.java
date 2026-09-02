package com.unicornt.store.infrastructure.web.mapper;

import com.unicornt.store.application.usecase.ordering.OrderConfirmation;
import com.unicornt.store.domain.model.Order;
import com.unicornt.store.domain.model.OrderItem;
import com.unicornt.store.domain.model.ShippingAddress;
import com.unicornt.store.infrastructure.web.dto.OrderDtos.OrderConfirmationResponse;
import com.unicornt.store.infrastructure.web.dto.OrderDtos.OrderLineResponse;
import com.unicornt.store.infrastructure.web.dto.OrderDtos.OrderResponse;
import com.unicornt.store.infrastructure.web.dto.OrderDtos.ShippingAddressRequest;
import com.unicornt.store.infrastructure.web.dto.OrderDtos.ShippingAddressResponse;

/** Translation between the order transport records and the domain model / use-case types. */
public final class OrderRestMapper {

    private OrderRestMapper() {
    }

    public static ShippingAddress toDomain(ShippingAddressRequest request) {
        return new ShippingAddress(request.street(), request.city(), request.region(), request.zipCode());
    }

    public static OrderConfirmationResponse toResponse(OrderConfirmation confirmation) {
        return new OrderConfirmationResponse(confirmation.id(), confirmation.status(), confirmation.total());
    }

    public static OrderResponse toResponse(Order order) {
        ShippingAddress address = order.shippingAddress();
        return new OrderResponse(
                order.id() == null ? 0L : order.id(),
                order.status().name(),
                order.total().amount(),
                order.createdAt(),
                new ShippingAddressResponse(address.street(), address.city(),
                        address.region(), address.zipCode()),
                order.items().stream().map(OrderRestMapper::toLine).toList());
    }

    private static OrderLineResponse toLine(OrderItem item) {
        return new OrderLineResponse(
                item.productId(),
                item.productName(),
                item.unitPrice().amount(),
                item.quantity(),
                item.subtotal().amount());
    }
}
