package com.unicornt.store.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@DisplayName("Quantity value object")
class QuantityTest {

    @Test
    @DisplayName("carries a positive count")
    void carriesValue() {
        assertThat(Quantity.of(3).value()).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -10})
    @DisplayName("rejects a non-positive count on construction")
    void rejectsNonPositive(int invalid) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Quantity.of(invalid))
                .withMessageContaining("greater than 0");
    }

    @Test
    @DisplayName("adds two quantities")
    void adds() {
        assertThat(Quantity.of(2).plus(Quantity.of(5))).isEqualTo(Quantity.of(7));
    }

    @Test
    @DisplayName("equality and hashCode are value based")
    void valueEquality() {
        assertThat(Quantity.of(4))
                .isEqualTo(Quantity.of(4))
                .hasSameHashCodeAs(Quantity.of(4))
                .isNotEqualTo(Quantity.of(5))
                .isNotEqualTo(null)
                .isNotEqualTo("4");
    }

    @Test
    @DisplayName("toString is the bare number")
    void readableToString() {
        assertThat(Quantity.of(9)).hasToString("9");
    }
}
