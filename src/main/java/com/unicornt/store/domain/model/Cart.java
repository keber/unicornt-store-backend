package com.unicornt.store.domain.model;

import com.unicornt.store.domain.valueobject.Quantity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A shopping cart, always scoped to a single user.
 *
 * <p>Plain Java. The aggregate owns its lines and every mutation goes through a
 * method that keeps the invariants (PLAN.md &sect;2.4):</p>
 * <ul>
 *   <li>every line quantity is strictly positive (enforced by {@link Quantity});</li>
 *   <li>a product appears at most once &mdash; adding a product already in the cart
 *       sums the quantity into the existing line;</li>
 *   <li>setting a line to zero removes it rather than storing a zero.</li>
 * </ul>
 *
 * <p>The cart carries the id of its owner. Whether a given caller may act on it is
 * the use case's decision; the aggregate only exposes the owner through
 * {@link #userId()} and {@link #belongsTo(String)}.</p>
 */
public final class Cart {

    private final String userId;
    private final List<CartItem> items;

    public Cart(String userId, List<CartItem> items) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("a cart is scoped to a user");
        }
        this.userId = userId;
        this.items = new ArrayList<>();
        if (items != null) {
            for (CartItem item : items) {
                addItem(item.productId(), item.quantity());
            }
        }
    }

    /** A cart with no lines for the given owner. */
    public static Cart empty(String userId) {
        return new Cart(userId, List.of());
    }

    public String userId() {
        return userId;
    }

    public boolean belongsTo(String candidateUserId) {
        return userId.equals(candidateUserId);
    }

    /** An unmodifiable snapshot of the lines, in insertion order. */
    public List<CartItem> items() {
        return List.copyOf(items);
    }

    public Optional<CartItem> findItem(long productId) {
        return items.stream().filter(item -> item.productId() == productId).findFirst();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    /** Total number of units across every line. */
    public int totalUnits() {
        return items.stream().mapToInt(item -> item.quantity().value()).sum();
    }

    /** Adds units for a product, summing into the existing line when there is one. */
    public void addItem(long productId, Quantity quantity) {
        int index = indexOf(productId);
        if (index < 0) {
            items.add(new CartItem(productId, quantity));
        } else {
            items.set(index, items.get(index).withAdditionalUnits(quantity.value()));
        }
    }

    /**
     * Sets the absolute quantity of a product's line, creating it if absent. A
     * non-positive target removes the line.
     */
    public void setItemQuantity(long productId, int units) {
        if (units <= 0) {
            removeItem(productId);
            return;
        }
        int index = indexOf(productId);
        if (index < 0) {
            items.add(CartItem.of(productId, units));
        } else {
            items.set(index, items.get(index).withQuantity(Quantity.of(units)));
        }
    }

    /**
     * Removes the line for a product.
     *
     * @return {@code true} if a line was present and removed, {@code false} otherwise
     */
    public boolean removeItem(long productId) {
        return items.removeIf(item -> item.productId() == productId);
    }

    /** Drops every line. */
    public void clear() {
        items.clear();
    }

    private int indexOf(long productId) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).productId() == productId) {
                return i;
            }
        }
        return -1;
    }
}
