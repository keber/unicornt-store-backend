package com.unicornt.store.application.usecase.cart;

import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.domain.model.Cart;
import com.unicornt.store.domain.repository.CartRepository;
import com.unicornt.store.domain.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Replaces the quantity of one line of the cart. A quantity of {@code 0} removes
 * the line; the line must already exist.
 */
@Service
public class UpdateCartItemUseCase {

    private final CartRepository carts;
    private final ProductRepository products;

    public UpdateCartItemUseCase(CartRepository carts, ProductRepository products) {
        this.carts = carts;
        this.products = products;
    }

    /**
     * @param quantity new absolute quantity; {@code 0} removes the line
     * @throws ResourceNotFoundException if the product is not a line of the cart
     */
    @Transactional
    public PricedCart execute(String userId, long productId, int quantity) {
        Cart cart = carts.findByUserId(userId);
        if (cart.findItem(productId).isEmpty()) {
            throw new ResourceNotFoundException("Cart item", productId);
        }
        cart.setItemQuantity(productId, quantity);
        return PricedCart.of(carts.save(cart), products);
    }
}
