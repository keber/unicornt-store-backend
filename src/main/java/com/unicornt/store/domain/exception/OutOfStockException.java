package com.unicornt.store.domain.exception;

/** Raised when a business rule about product availability is violated. Maps to HTTP 422. */
public class OutOfStockException extends RuntimeException {

    public OutOfStockException(Object productId) {
        super("Out of stock for product " + productId);
    }
}
