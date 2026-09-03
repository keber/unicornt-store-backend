package com.unicornt.store.application.usecase.ordering;

/** The minimal result of placing an order: what {@code POST /api/v1/orders} returns. */
public record OrderConfirmation(long id, String status, int total) {
}
