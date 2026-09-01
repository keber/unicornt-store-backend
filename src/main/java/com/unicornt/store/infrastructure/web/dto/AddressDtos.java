package com.unicornt.store.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request and response payloads of the address resource. */
public final class AddressDtos {

    private AddressDtos() {
    }

    /** Body of {@code POST /api/v1/addresses}. */
    @Schema(name = "AddressCreateRequest", description = "Shipping address to register for the authenticated user")
    public record AddressCreateRequest(

            @Schema(description = "Street and number", example = "Av. Providencia 1234")
            @NotBlank(message = "street is required")
            @Size(max = 200, message = "street must be at most 200 characters")
            String street,

            @Schema(description = "City", example = "Santiago")
            @NotBlank(message = "city is required")
            @Size(max = 100, message = "city must be at most 100 characters")
            String city,

            @Schema(description = "Region or state", example = "Region Metropolitana")
            @NotBlank(message = "region is required")
            @Size(max = 100, message = "region must be at most 100 characters")
            String region,

            @Schema(description = "Postal code", example = "7500000")
            @Size(max = 20, message = "zipCode must be at most 20 characters")
            String zipCode) {
    }

    /** A registered shipping address. */
    @Schema(name = "AddressResponse", description = "Shipping address of the authenticated user")
    public record AddressResponse(

            @Schema(description = "Identifier of the address", example = "7")
            Long id,

            @Schema(description = "Street and number", example = "Av. Providencia 1234")
            String street,

            @Schema(description = "City", example = "Santiago")
            String city,

            @Schema(description = "Region or state", example = "Region Metropolitana")
            String region,

            @Schema(description = "Postal code", example = "7500000")
            String zipCode,

            @Schema(description = "Whether this is the default address of the user", example = "true")
            boolean isDefault,

            @Schema(description = "Address rendered as a single line",
                    example = "Av. Providencia 1234, Santiago, Region Metropolitana 7500000")
            String fullAddress) {
    }
}
