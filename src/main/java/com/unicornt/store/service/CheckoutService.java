package com.unicornt.store.service;

import com.unicornt.store.model.CartItem;

import java.util.List;

/** Checkout use cases extracted from the removed checkout controller. */
public interface CheckoutService {

    /**
     * Confirms the checkout for a user against one of that user's addresses:
     * validates the cart is not empty, computes the total and empties the cart.
     *
     * @throws com.unicornt.store.domain.exception.ResourceNotFoundException if the address does not
     *                                                                       belong to the user
     */
    OrderSummary confirm(String userEmail, Long addressId);

    /** Immutable result of a confirmed checkout. */
    record OrderSummary(String shippingAddress, int total, int itemCount, List<CartItem> items) {
    }
}
