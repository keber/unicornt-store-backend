package com.unicornt.store.application.usecase.cart;

import com.unicornt.store.domain.repository.CartRepository;
import com.unicornt.store.domain.repository.ProductRepository;
import org.springframework.stereotype.Service;

/** Returns the cart of the authenticated user, priced against the current catalog. */
@Service
public class GetCartUseCase {

    private final CartRepository carts;
    private final ProductRepository products;

    public GetCartUseCase(CartRepository carts, ProductRepository products) {
        this.carts = carts;
        this.products = products;
    }

    public PricedCart execute(String userId) {
        return PricedCart.of(carts.findByUserId(userId), products);
    }
}
