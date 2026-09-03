package com.unicornt.store.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

@DisplayName("Money value object")
class MoneyTest {

    @Test
    @DisplayName("carries a whole CLP amount")
    void carriesAmount() {
        assertThat(Money.ofClp(14990).amount()).isEqualTo(14990);
    }

    @Test
    @DisplayName("accepts zero but reports it as not positive")
    void zeroIsAllowedButNotPositive() {
        Money zero = Money.ofClp(0);

        assertThat(zero.amount()).isZero();
        assertThat(zero.isPositive()).isFalse();
    }

    @Test
    @DisplayName("a positive amount reports as positive")
    void positiveAmount() {
        assertThat(Money.ofClp(1).isPositive()).isTrue();
    }

    @Test
    @DisplayName("rejects a negative amount on construction")
    void rejectsNegative() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Money.ofClp(-1))
                .withMessageContaining("must not be negative");
    }

    @Test
    @DisplayName("adds two amounts")
    void adds() {
        assertThat(Money.ofClp(1000).plus(Money.ofClp(250))).isEqualTo(Money.ofClp(1250));
    }

    @Test
    @DisplayName("multiplies by a non-negative factor")
    void multiplies() {
        assertThat(Money.ofClp(1000).times(3)).isEqualTo(Money.ofClp(3000));
        assertThat(Money.ofClp(1000).times(0)).isEqualTo(Money.ofClp(0));
    }

    @Test
    @DisplayName("rejects multiplication by a negative factor")
    void rejectsNegativeFactor() {
        Money base = Money.ofClp(1000);
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> base.times(-1))
                .withMessageContaining("must not be negative");
    }

    @Test
    @DisplayName("equality and hashCode are value based")
    void valueEquality() {
        assertThat(Money.ofClp(500))
                .isEqualTo(Money.ofClp(500))
                .hasSameHashCodeAs(Money.ofClp(500))
                .isNotEqualTo(Money.ofClp(501))
                .isNotEqualTo(null)
                .isNotEqualTo("500");
    }

    @Test
    @DisplayName("toString shows the currency and amount")
    void readableToString() {
        assertThat(Money.ofClp(990)).hasToString("CLP 990");
    }

    @Test
    @DisplayName("a zero-amount construction never throws")
    void zeroConstructionSafe() {
        assertThatNoException().isThrownBy(() -> Money.ofClp(0));
    }
}
