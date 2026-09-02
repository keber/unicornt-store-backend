package com.unicornt.store.application.usecase.cart;

import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.domain.model.Cart;
import com.unicornt.store.domain.model.Product;
import com.unicornt.store.domain.repository.CartRepository;
import com.unicornt.store.domain.repository.ProductRepository;
import com.unicornt.store.domain.valueobject.Quantity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adds a product to the cart of the authenticated user, or increases the quantity
 * of the line when the product is already there.
 */
@Service
public class AddCartItemUseCase {

    private final CartRepository carts;
    private final ProductRepository products;

    public AddCartItemUseCase(CartRepository carts, ProductRepository products) {
        this.carts = carts;
        this.products = products;
    }

    /**
     * @param quantity units to add; must be strictly positive
     * @throws ResourceNotFoundException if the product does not exist
     * @throws IllegalArgumentException  if {@code quantity} is not positive
     */
    @Transactional
    public Result execute(String userId, long productId, int quantity) {
        Product product = products.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        Cart cart = carts.findByUserId(userId);
        boolean created = cart.findItem(product.id()).isEmpty();
        cart.addItem(product.id(), Quantity.of(quantity));
        Cart saved = carts.save(cart);
        return new Result(PricedCart.of(saved, products), created);
    }

    /** The updated cart plus whether a brand-new line was created (drives 201 vs 200). */
    public record Result(PricedCart cart, boolean created) {
    }
}
