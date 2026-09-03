package com.unicornt.store.domain.repository;

import com.unicornt.store.domain.model.Cart;

/**
 * Port for shopping-cart persistence. Pure domain types in and out; the JPA
 * adapter in {@code infrastructure.persistence.adapter} implements it.
 *
 * <p>The cart is addressed by {@code userId}, the identity of the authenticated
 * principal (the user's email in this application). Translating that identity to
 * whatever key the storage uses is the adapter's concern, not the domain's.</p>
 */
public interface CartRepository {

    /**
     * The cart of a user. A user that has never added anything gets an empty cart,
     * never {@code null}.
     */
    Cart findByUserId(String userId);

    /** Persists the cart as the new full state for its owner and returns it re-read. */
    Cart save(Cart cart);

    /** Empties the cart of a user. A no-op when the cart is already empty. */
    void deleteByUserId(String userId);
}
