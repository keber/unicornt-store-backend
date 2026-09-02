package com.unicornt.store.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/** Request and response payloads of the order resource. */
public final class OrderDtos {

    private OrderDtos() {
    }

    /** Shipping address carried inline by {@code POST /api/v1/orders}. */
    @Schema(name = "ShippingAddressRequest", description = "Where the order ships")
    public record ShippingAddressRequest(

            @Schema(description = "Street and number", example = "Av. Providencia 1234",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "street is required")
            @Size(max = 200, message = "street must not exceed 200 characters")
            String street,

            @Schema(description = "City", example = "Santiago", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "city is required")
            @Size(max = 100, message = "city must not exceed 100 characters")
            String city,

            @Schema(description = "Region / state", example = "Region Metropolitana",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "region is required")
            @Size(max = 100, message = "region must not exceed 100 characters")
            String region,

            @Schema(description = "Postal code (optional)", example = "7500000")
            @Size(max = 20, message = "zipCode must not exceed 20 characters")
            String zipCode) {
    }

    /** Body of {@code POST /api/v1/orders}: confirms the current cart. */
    @Schema(name = "PlaceOrderRequest", description = "Confirm the current cart into an order")
    public record PlaceOrderRequest(

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull(message = "shippingAddress is required")
            @Valid
            ShippingAddressRequest shippingAddress) {
    }

    /** Response of {@code POST /api/v1/orders}. */
    @Schema(name = "OrderConfirmationResponse", description = "Result of confirming a cart")
    public record OrderConfirmationResponse(

            @Schema(description = "Identifier of the new order", example = "58")
            long id,

            @Schema(description = "Order status", example = "CONFIRMED")
            String status,

            @Schema(description = "Order total in whole CLP units", example = "44970")
            int total) {
    }

    /** One line of a confirmed order. */
    @Schema(name = "OrderLineResponse", description = "A single line of a confirmed order")
    public record OrderLineResponse(

            @Schema(description = "Identifier of the purchased product", example = "12")
            long productId,

            @Schema(description = "Product name frozen at purchase time", example = "Rainbow Mug")
            String productName,

            @Schema(description = "Unit price frozen at purchase time, whole CLP units", example = "7990")
            int unitPrice,

            @Schema(description = "Purchased units", example = "2")
            int quantity,

            @Schema(description = "Unit price multiplied by the quantity", example = "15980")
            int subtotal) {
    }

    /** Shipping address as stored on the order. */
    @Schema(name = "ShippingAddressResponse", description = "Shipping address snapshot")
    public record ShippingAddressResponse(String street, String city, String region, String zipCode) {
    }

    /** Full representation of a confirmed order (list and get-by-id). */
    @Schema(name = "OrderResponse", description = "A confirmed order")
    public record OrderResponse(

            @Schema(description = "Identifier of the order", example = "58")
            long id,

            @Schema(description = "Order status", example = "CONFIRMED")
            String status,

            @Schema(description = "Order total in whole CLP units", example = "44970")
            int total,

            @Schema(description = "Moment the order was confirmed, in UTC")
            Instant createdAt,

            @Schema(description = "Shipping address snapshot")
            ShippingAddressResponse shippingAddress,

            @Schema(description = "Lines of the order")
            List<OrderLineResponse> items) {
    }
}
