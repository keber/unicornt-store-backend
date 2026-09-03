package com.unicornt.store.domain.model;

import com.unicornt.store.domain.valueobject.Money;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * An order confirmed from a cart. Plain Java. Invariants: an order always belongs to
 * a user, always ships somewhere, and always has at least one line; the total is the
 * sum of the line subtotals, computed here, never taken from the caller.
 */
public final class Order {

    private final Long id;
    private final String userId;
    private final ShippingAddress shippingAddress;
    private final List<OrderItem> items;
    private final Money total;
    private final OrderStatus status;
    private final Instant createdAt;

    public Order(Long id, String userId, ShippingAddress shippingAddress,
                 List<OrderItem> items, Money total, OrderStatus status, Instant createdAt) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("an order must belong to a user");
        }
        Objects.requireNonNull(shippingAddress, "shippingAddress is required");
        Objects.requireNonNull(items, "items is required");
        if (items.isEmpty()) {
            throw new IllegalArgumentException("an order has at least one line");
        }
        Objects.requireNonNull(total, "total is required");
        Objects.requireNonNull(status, "status is required");
        Objects.requireNonNull(createdAt, "createdAt is required");
        this.id = id;
        this.userId = userId;
        this.shippingAddress = shippingAddress;
        this.items = List.copyOf(items);
        this.total = total;
        this.status = status;
        this.createdAt = createdAt;
    }

    /** Places a brand-new order: payment is simulated, so it is born {@code CONFIRMED}. */
    public static Order place(String userId, ShippingAddress shippingAddress, List<OrderItem> items) {
        Money total = items.stream()
                .map(OrderItem::subtotal)
                .reduce(Money.ofClp(0), Money::plus);
        return new Order(null, userId, shippingAddress, items, total, OrderStatus.CONFIRMED, Instant.now());
    }

    public Long id() {
        return id;
    }

    public String userId() {
        return userId;
    }

    public ShippingAddress shippingAddress() {
        return shippingAddress;
    }

    public List<OrderItem> items() {
        return items;
    }

    public Money total() {
        return total;
    }

    public OrderStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
