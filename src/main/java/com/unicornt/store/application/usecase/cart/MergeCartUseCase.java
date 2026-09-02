package com.unicornt.store.application.usecase.cart;

import com.unicornt.store.domain.model.Cart;
import com.unicornt.store.domain.model.Product;
import com.unicornt.store.domain.repository.CartRepository;
import com.unicornt.store.domain.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Folds a client's local (anonymous) cart into the authenticated user's
 * server-side cart on login.
 *
 * <p>For each incoming {@code {productId, quantity}} the new quantity is
 * {@code server_qty + local_qty}, then clamped to the product's current stock.
 * Incoming lines whose product no longer exists, or whose clamped quantity would
 * be zero (no stock), are dropped. The clamp can lower a pre-existing server line
 * when stock has since fallen below it.</p>
 */
@Service
public class MergeCartUseCase {

    private final CartRepository carts;
    private final ProductRepository products;

    public MergeCartUseCase(CartRepository carts, ProductRepository products) {
        this.carts = carts;
        this.products = products;
    }

    @Transactional
    public PricedCart execute(String userId, List<IncomingItem> incoming) {
        Cart cart = carts.findByUserId(userId);
        for (IncomingItem line : incoming) {
            Product product = products.findById(line.productId()).orElse(null);
            if (product == null) {
                continue;
            }
            int current = cart.findItem(product.id())
                    .map(item -> item.quantity().value())
                    .orElse(0);
            int clamped = Math.min(current + line.quantity(), product.stock());
            if (clamped <= 0) {
                continue;
            }
            cart.setItemQuantity(product.id(), clamped);
        }
        return PricedCart.of(carts.save(cart), products);
    }

    /** One line of the local cart being merged in. */
    public record IncomingItem(long productId, int quantity) {

        public IncomingItem {
            if (productId <= 0) {
                throw new IllegalArgumentException("productId must be greater than 0");
            }
            if (quantity <= 0) {
                throw new IllegalArgumentException("quantity must be greater than 0");
            }
        }
    }
}
