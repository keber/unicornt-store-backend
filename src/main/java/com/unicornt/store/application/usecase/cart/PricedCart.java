package com.unicornt.store.application.usecase.cart;

import com.unicornt.store.domain.model.Cart;
import com.unicornt.store.domain.model.CartItem;
import com.unicornt.store.domain.model.Product;
import com.unicornt.store.domain.repository.ProductRepository;
import com.unicornt.store.domain.valueobject.Money;

import java.util.ArrayList;
import java.util.List;

/**
 * A cart with every amount resolved against the current catalog: the read model
 * the cart endpoints return. Lines whose product no longer exists are dropped,
 * exactly as the legacy cart did, so a deleted product never breaks the cart.
 *
 * @param items      priced lines, in cart order
 * @param itemCount  total number of units across every line
 * @param total      sum of every line subtotal
 */
public record PricedCart(List<Line> items, int itemCount, Money total) {

    public PricedCart {
        items = List.copyOf(items);
    }

    /** One priced line of the cart. */
    public record Line(long productId, String productName, String imageBase,
                       Money unitPrice, int quantity, Money subtotal) {
    }

    /** Prices {@code cart} against {@code products}, skipping lines with no live product. */
    public static PricedCart of(Cart cart, ProductRepository products) {
        List<Line> lines = new ArrayList<>();
        int itemCount = 0;
        Money total = Money.ofClp(0);
        for (CartItem item : cart.items()) {
            Product product = products.findById(item.productId()).orElse(null);
            if (product == null) {
                continue;
            }
            int quantity = item.quantity().value();
            Money subtotal = product.price().times(quantity);
            lines.add(new Line(product.id(), product.name(), product.imageBase(),
                    product.price(), quantity, subtotal));
            itemCount += quantity;
            total = total.plus(subtotal);
        }
        return new PricedCart(lines, itemCount, total);
    }
}
