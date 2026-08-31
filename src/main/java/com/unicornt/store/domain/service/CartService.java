package com.unicornt.store.domain.service;

import com.unicornt.store.infrastructure.persistence.entity.CartItemEntity;

import java.math.BigDecimal;
import java.util.List;

/**
 * Shopping cart use cases, always scoped to the authenticated user. The user is identified
 * by email; no caller may address another user's cart.
 */
public interface CartService {

    /** Returns the cart of a user with every monetary amount already computed. */
    CartView getCart(String userEmail);

    /**
     * Adds a product to the cart, or increases the quantity when the line already exists.
     *
     * @throws com.unicornt.store.domain.exception.ResourceNotFoundException if the product does not exist
     */
    CartLine addItem(String userEmail, int productId, int quantity);

    /**
     * Replaces the quantity of one line of the user's cart.
     *
     * @throws com.unicornt.store.domain.exception.ResourceNotFoundException if the line is not the user's
     */
    CartLine updateItemQuantity(String userEmail, Long cartItemId, int quantity);

    /**
     * Removes one line of the user's cart.
     *
     * @throws com.unicornt.store.domain.exception.ResourceNotFoundException if the line is not the user's
     */
    void removeItem(String userEmail, Long cartItemId);

    /** Empties the cart of a user. Never fails on an already empty cart. */
    void clearCart(String userEmail);

    /** Raw cart rows with their product resolved, used by checkout. */
    List<CartItemEntity> getCartItems(String userEmail);

    /** One priced line of a cart. */
    record CartLine(Long id, int productId, String productName, String imageBase,
                    BigDecimal unitPrice, int quantity, BigDecimal subtotal) {
    }

    /** A whole priced cart. */
    record CartView(List<CartLine> items, int itemCount, BigDecimal total) {
    }
}
