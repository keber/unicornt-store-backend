package com.unicornt.store.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@DisplayName("ProductType domain model")
class ProductTypeTest {

    @Test
    @DisplayName("carries id, name and slug")
    void carriesFields() {
        ProductType type = new ProductType(1L, "T-shirt", "t-shirt");

        assertThat(type.id()).isEqualTo(1L);
        assertThat(type.name()).isEqualTo("T-shirt");
        assertThat(type.slug()).isEqualTo("t-shirt");
    }

    @Test
    @DisplayName("rejects a blank or null name")
    void requiresName() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new ProductType(1L, "  ", "s"))
                .withMessageContaining("name is required");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new ProductType(1L, null, "s"))
                .withMessageContaining("name is required");
    }
}
