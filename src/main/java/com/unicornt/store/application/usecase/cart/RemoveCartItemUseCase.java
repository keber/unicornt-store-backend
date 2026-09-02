package com.unicornt.store.application.usecase.cart;

import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.domain.model.Cart;
import com.unicornt.store.domain.repository.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Removes one line from the cart of the authenticated user. */
@Service
public class RemoveCartItemUseCase {

    private final CartRepository carts;

    public RemoveCartItemUseCase(CartRepository carts) {
        this.carts = carts;
    }

    /**
     * @throws ResourceNotFoundException if the product is not a line of the cart
     */
    @Transactional
    public void execute(String userId, long productId) {
        Cart cart = carts.findByUserId(userId);
        if (!cart.removeItem(productId)) {
            throw new ResourceNotFoundException("Cart item", productId);
        }
        carts.save(cart);
    }
}
