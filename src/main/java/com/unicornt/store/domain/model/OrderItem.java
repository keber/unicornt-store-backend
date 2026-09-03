package com.unicornt.store.domain.model;

import com.unicornt.store.domain.valueobject.Money;

/**
 * One line of a confirmed order. {@code productName} and {@code unitPrice} are a
 * snapshot frozen at purchase time; a later catalog change never rewrites them.
 */
public record OrderItem(long productId, String productName, Money unitPrice, int quantity) {

    public OrderItem {
        if (productId <= 0) {
            throw new IllegalArgumentException("productId must be positive");
        }
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("productName is required");
        }
        if (unitPrice == null) {
            throw new IllegalArgumentException("unitPrice is required");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }
        productName = productName.trim();
    }

    public Money subtotal() {
        return unitPrice.times(quantity);
    }
}
