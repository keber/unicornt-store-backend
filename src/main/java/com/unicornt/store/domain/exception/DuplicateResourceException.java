package com.unicornt.store.domain.exception;

/** Raised when creating a resource that collides with an existing one. Maps to HTTP 409. */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String resource, String field, Object value) {
        super("%s already exists with %s: %s".formatted(resource, field, value));
    }

    public DuplicateResourceException(String message) {
        super(message);
    }
}
