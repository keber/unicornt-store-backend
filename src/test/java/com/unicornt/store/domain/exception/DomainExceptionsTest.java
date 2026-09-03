package com.unicornt.store.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Covers both constructor shapes of every domain exception. */
@DisplayName("domain exceptions")
class DomainExceptionsTest {

    @Test
    @DisplayName("ResourceNotFoundException formats the resource+id and takes a raw message")
    void resourceNotFound() {
        assertThat(new ResourceNotFoundException("Product", 42).getMessage()).isEqualTo("Product not found: 42");
        assertThat(new ResourceNotFoundException("custom message").getMessage()).isEqualTo("custom message");
    }

    @Test
    @DisplayName("DuplicateResourceException formats the field+value and takes a raw message")
    void duplicateResource() {
        assertThat(new DuplicateResourceException("User", "email", "a@b.cl").getMessage())
                .isEqualTo("User already exists with email: a@b.cl");
        assertThat(new DuplicateResourceException("already there").getMessage()).isEqualTo("already there");
    }

    @Test
    @DisplayName("OutOfStockException names the product")
    void outOfStock() {
        assertThat(new OutOfStockException(7).getMessage()).contains("7");
    }
}
