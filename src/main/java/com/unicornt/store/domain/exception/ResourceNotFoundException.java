package com.unicornt.store.domain.exception;

/** Raised when a requested domain resource does not exist. Maps to HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super("%s not found: %s".formatted(resource, id));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
