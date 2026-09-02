package com.unicornt.store.domain.model;

/** Lifecycle of an order. Payment is simulated, so a fresh order is already {@code CONFIRMED}. */
public enum OrderStatus {
    CONFIRMED,
    CANCELLED
}
