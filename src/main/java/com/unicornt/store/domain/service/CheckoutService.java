package com.unicornt.store.domain.service;

import com.unicornt.store.infrastructure.persistence.entity.OrderEntity;

import java.util.List;

/** Order use cases: confirming a cart into an order and reading the caller's own orders. */
public interface CheckoutService {

    /**
     * Confirms the cart of a user against one of that user's addresses: validates the cart is
     * not empty, reserves stock for every line, stores the order and empties the cart. The whole
     * sequence is one transaction, so a failed line never leaves stock decremented.
     *
     * @throws IllegalArgumentException                                       if the cart is empty
     * @throws com.unicornt.store.domain.exception.ResourceNotFoundException  if the address is not the user's
     * @throws com.unicornt.store.domain.exception.OutOfStockException        if a product has not enough stock
     */
    OrderEntity confirm(String userEmail, Long addressId);

    /** Orders of the given user, most recent first. */
    List<OrderEntity> findOrders(String userEmail);

    /**
     * One order, only if it belongs to the given user.
     *
     * @throws com.unicornt.store.domain.exception.ResourceNotFoundException otherwise
     */
    OrderEntity findOrder(String userEmail, Long orderId);
}
