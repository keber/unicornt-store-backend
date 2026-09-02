package com.unicornt.store.application.usecase.cart;

import com.unicornt.store.domain.model.Product;
import com.unicornt.store.domain.valueobject.Money;

/** Shared object mothers for the cart use-case tests. */
final class CartUseCaseFixtures {

    static final String USER = "buyer@unicornt.dev";

    private CartUseCaseFixtures() {
    }

    static Product product(long id, int priceClp, int stock) {
        return new Product(id, "Product " + id, "d", "image-" + id, Money.ofClp(priceClp),
                1L, "Unicorns", 1L, "Plush", stock, true);
    }
}
