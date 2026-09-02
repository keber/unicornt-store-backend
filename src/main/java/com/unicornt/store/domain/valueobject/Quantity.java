package com.unicornt.store.domain.valueobject;

import java.util.Objects;

/**
 * A count of units of a single product &mdash; in a cart line or an order line.
 *
 * <p>Plain Java, no framework. The invariant is enforced in the constructor: a
 * quantity is a strictly positive integer. "Remove the line" is modelled by the
 * aggregate dropping the item, never by a {@code Quantity} of zero.</p>
 */
public final class Quantity {

    private final int value;

    private Quantity(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0: " + value);
        }
        this.value = value;
    }

    public static Quantity of(int value) {
        return new Quantity(value);
    }

    public int value() {
        return value;
    }

    public Quantity plus(Quantity other) {
        return new Quantity(this.value + other.value);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Quantity quantity && quantity.value == this.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
