package com.unicornt.store.infrastructure.web.mapper;

import com.unicornt.store.domain.service.CartService.CartLine;
import com.unicornt.store.domain.service.CartService.CartView;
import com.unicornt.store.infrastructure.web.dto.CartDtos.CartItemResponse;
import com.unicornt.store.infrastructure.web.dto.CartDtos.CartResponse;

import java.util.List;

/**
 * Translation between the priced cart returned by the service and the cart DTOs.
 * Every amount arrives already computed; the mapper never does arithmetic.
 */
public final class CartMapper {

    private CartMapper() {
    }

    public static CartItemResponse toResponse(CartLine line) {
        return new CartItemResponse(line.id(), line.productId(), line.productName(),
                line.imageBase(), line.unitPrice(), line.quantity(), line.subtotal());
    }

    public static CartResponse toResponse(CartView cart) {
        List<CartItemResponse> items = cart.items().stream()
                .map(CartMapper::toResponse)
                .toList();
        return new CartResponse(items, cart.itemCount(), cart.total());
    }
}
