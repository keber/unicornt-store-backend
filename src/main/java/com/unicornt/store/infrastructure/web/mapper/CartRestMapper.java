package com.unicornt.store.infrastructure.web.mapper;

import com.unicornt.store.application.usecase.cart.MergeCartUseCase.IncomingItem;
import com.unicornt.store.application.usecase.cart.PricedCart;
import com.unicornt.store.infrastructure.web.dto.CartDtos.CartItemResponse;
import com.unicornt.store.infrastructure.web.dto.CartDtos.CartResponse;
import com.unicornt.store.infrastructure.web.dto.CartDtos.MergeCartRequest;

import java.util.List;

/**
 * Translation between the cart transport records and the priced cart / use-case
 * input. Pure static methods; every amount arrives already computed, so the
 * mapper never does arithmetic.
 */
public final class CartRestMapper {

    private CartRestMapper() {
    }

    public static CartResponse toResponse(PricedCart cart) {
        List<CartItemResponse> items = cart.items().stream()
                .map(CartRestMapper::toItemResponse)
                .toList();
        return new CartResponse(items, cart.itemCount(), cart.total().amount());
    }

    public static CartItemResponse toItemResponse(PricedCart.Line line) {
        return new CartItemResponse(
                line.productId(),
                line.productName(),
                line.imageBase(),
                line.unitPrice().amount(),
                line.quantity(),
                line.subtotal().amount());
    }

    public static List<IncomingItem> toIncomingItems(MergeCartRequest request) {
        return request.items().stream()
                .map(item -> new IncomingItem(item.productId(), item.quantity()))
                .toList();
    }
}
