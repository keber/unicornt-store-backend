package com.unicornt.store.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@DisplayName("ShippingAddress value object")
class ShippingAddressTest {

    @Test
    @DisplayName("trims the fields and keeps a supplied zip code")
    void trimsAndKeepsZip() {
        ShippingAddress address = new ShippingAddress("  Av. 1234 ", " Santiago", "RM ", " 7500000 ");

        assertThat(address.street()).isEqualTo("Av. 1234");
        assertThat(address.city()).isEqualTo("Santiago");
        assertThat(address.region()).isEqualTo("RM");
        assertThat(address.zipCode()).isEqualTo("7500000");
    }

    @Test
    @DisplayName("normalises a blank or null zip code to null")
    void blankZipBecomesNull() {
        assertThat(new ShippingAddress("s", "c", "r", "   ").zipCode()).isNull();
        assertThat(new ShippingAddress("s", "c", "r", null).zipCode()).isNull();
    }

    @Test
    @DisplayName("requires street, city and region")
    void requiresCoreFields() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new ShippingAddress("  ", "c", "r", null)).withMessageContaining("street is required");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new ShippingAddress("s", null, "r", null)).withMessageContaining("city is required");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new ShippingAddress("s", "c", "", null)).withMessageContaining("region is required");
    }

    @Test
    @DisplayName("oneLine renders with and without the zip code")
    void oneLine() {
        assertThat(new ShippingAddress("Av. 1234", "Santiago", "RM", "7500000").oneLine())
                .isEqualTo("Av. 1234, Santiago, RM 7500000");
        assertThat(new ShippingAddress("Av. 1234", "Santiago", "RM", null).oneLine())
                .isEqualTo("Av. 1234, Santiago, RM");
    }
}
