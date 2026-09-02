package com.unicornt.store.infrastructure.web.mapper;

import com.unicornt.store.infrastructure.persistence.entity.AddressEntity;
import com.unicornt.store.infrastructure.web.dto.AddressDtos.AddressCreateRequest;
import com.unicornt.store.infrastructure.web.dto.AddressDtos.AddressResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit test of the pure static translation between address records and the address entity. */
class AddressMapperTest {

    private static AddressEntity anAddressEntity() {
        AddressEntity address = new AddressEntity();
        address.setId(7L);
        address.setUserId(1L);
        address.setStreet("Av. Providencia 1234");
        address.setCity("Santiago");
        address.setRegion("Region Metropolitana");
        address.setZipCode("7500000");
        address.setDefault(true);
        return address;
    }

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("copies the four request fields and leaves owner and default flag unset")
        void mapsRequestFields() {
            AddressCreateRequest request = new AddressCreateRequest(
                    "Av. Providencia 1234", "Santiago", "Region Metropolitana", "7500000");

            AddressEntity address = AddressMapper.toEntity(request);

            assertThat(address.getStreet()).isEqualTo("Av. Providencia 1234");
            assertThat(address.getCity()).isEqualTo("Santiago");
            assertThat(address.getRegion()).isEqualTo("Region Metropolitana");
            assertThat(address.getZipCode()).isEqualTo("7500000");
            assertThat(address.getId()).isNull();
            assertThat(address.getUserId()).isNull();
            assertThat(address.isDefault()).isFalse();
        }

        @Test
        @DisplayName("passes a null zip code through unchanged")
        void nullZipCode() {
            AddressCreateRequest request = new AddressCreateRequest(
                    "street", "city", "region", null);

            assertThat(AddressMapper.toEntity(request).getZipCode()).isNull();
        }
    }

    @Nested
    @DisplayName("toResponse")
    class ToResponse {

        @Test
        @DisplayName("copies every field and renders the entity's single-line address")
        void mapsAllFields() {
            AddressResponse response = AddressMapper.toResponse(anAddressEntity());

            assertThat(response.id()).isEqualTo(7L);
            assertThat(response.street()).isEqualTo("Av. Providencia 1234");
            assertThat(response.city()).isEqualTo("Santiago");
            assertThat(response.region()).isEqualTo("Region Metropolitana");
            assertThat(response.zipCode()).isEqualTo("7500000");
            assertThat(response.isDefault()).isTrue();
            assertThat(response.fullAddress())
                    .isEqualTo("Av. Providencia 1234, Santiago, Region Metropolitana 7500000");
        }

        @Test
        @DisplayName("omits a blank zip code from the single-line address and preserves a non-default flag")
        void blankZipCodeAndNonDefault() {
            AddressEntity address = anAddressEntity();
            address.setZipCode(null);
            address.setDefault(false);

            AddressResponse response = AddressMapper.toResponse(address);

            assertThat(response.isDefault()).isFalse();
            assertThat(response.fullAddress())
                    .isEqualTo("Av. Providencia 1234, Santiago, Region Metropolitana");
        }
    }
}
