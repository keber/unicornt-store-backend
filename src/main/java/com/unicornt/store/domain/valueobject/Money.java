package com.unicornt.store.domain.valueobject;

import java.util.Objects;

/**
 * A monetary amount in whole Chilean peso units (the store never deals in cents).
 *
 * <p>Plain Java, no framework. The invariant &mdash; an amount is never negative &mdash;
 * is enforced in the constructor, so a {@code Money} instance cannot exist in an
 * invalid state. A zero amount is allowed; callers that need a strictly positive
 * price ask with {@link #isPositive()}.</p>
 */
public final class Money {

    private final int amount;

    private Money(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("money amount must not be negative: " + amount);
        }
        this.amount = amount;
    }

    /** Amount in whole CLP units. */
    public static Money ofClp(int amount) {
        return new Money(amount);
    }

    public int amount() {
        return amount;
    }

    public boolean isPositive() {
        return amount > 0;
    }

    public Money plus(Money other) {
        return new Money(this.amount + other.amount);
    }

    /** This amount taken {@code times} over; {@code times} must not be negative. */
    public Money times(int factor) {
        if (factor < 0) {
            throw new IllegalArgumentException("multiplier must not be negative: " + factor);
        }
        return new Money(this.amount * factor);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Money money && money.amount == this.amount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount);
    }

    @Override
    public String toString() {
        return "CLP " + amount;
    }
}
