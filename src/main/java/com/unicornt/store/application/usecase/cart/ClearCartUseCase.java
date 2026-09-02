package com.unicornt.store.application.usecase.cart;

import com.unicornt.store.domain.repository.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Empties the cart of the authenticated user. Idempotent: clearing an already
 * empty cart is a no-op. Used by checkout once an order is confirmed.
 */
@Service
public class ClearCartUseCase {

    private final CartRepository carts;

    public ClearCartUseCase(CartRepository carts) {
        this.carts = carts;
    }

    @Transactional
    public void execute(String userId) {
        carts.deleteByUserId(userId);
    }
}
