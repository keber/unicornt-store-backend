package com.unicornt.store.domain.model;

import com.unicornt.store.domain.valueobject.Quantity;

import java.util.Objects;

/**
 * One line of a {@link Cart}: a product and how many units of it the shopper wants.
 *
 * <p>Plain Java. The invariants are: a positive product reference and a strictly
 * positive {@link Quantity} (a zero quantity is modelled by the {@link Cart}
 * dropping the line, never by a {@code CartItem} of zero). Product name, image and
 * price are <em>not</em> held here; they are read-model data the application layer
 * joins in from the catalog when it prices the cart.</p>
 */
public final class CartItem {

    private final long productId;
    private final Quantity quantity;

    public CartItem(long productId, Quantity quantity) {
        if (productId <= 0) {
            throw new IllegalArgumentException("a cart item must reference a product");
        }
        this.quantity = Objects.requireNonNull(quantity, "quantity is required");
        this.productId = productId;
    }

    public static CartItem of(long productId, int units) {
        return new CartItem(productId, Quantity.of(units));
    }

    /** The same line with {@code units} added to the current quantity. */
    CartItem withAdditionalUnits(int units) {
        return new CartItem(productId, quantity.plus(Quantity.of(units)));
    }

    /** The same line at an absolute quantity. */
    CartItem withQuantity(Quantity newQuantity) {
        return new CartItem(productId, newQuantity);
    }

    public long productId() {
        return productId;
    }

    public Quantity quantity() {
        return quantity;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CartItem item
                && item.productId == this.productId
                && item.quantity.equals(this.quantity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, quantity);
    }

    @Override
    public String toString() {
        return "CartItem{productId=" + productId + ", quantity=" + quantity + '}';
    }
}
