package com.unicornt.store.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Request and response payloads of the order resource. */
public final class OrderDtos {

    private OrderDtos() {
    }

    /** Body of {@code POST /api/v1/orders}: confirms the current cart. */
    @Schema(name = "OrderCreateRequest", description = "Address the confirmed cart is shipped to")
    public record OrderCreateRequest(

            @Schema(description = "Identifier of one of the addresses of the caller", example = "7")
            @NotNull(message = "addressId is required")
            @Positive(message = "addressId must be greater than 0")
            Long addressId) {
    }

    /** One line of a confirmed order. */
    @Schema(name = "OrderLineResponse", description = "A single line of a confirmed order")
    public record OrderLineResponse(

            @Schema(description = "Identifier of the line", example = "101")
            Long id,

            @Schema(description = "Identifier of the purchased product", example = "12")
            int productId,

            @Schema(description = "Product name frozen at purchase time", example = "Unicorn plush")
            String productName,

            @Schema(description = "Unit price frozen at purchase time", example = "14990")
            BigDecimal unitPrice,

            @Schema(description = "Purchased units", example = "2")
            int quantity,

            @Schema(description = "Unit price multiplied by the quantity", example = "29980")
            BigDecimal subtotal) {
    }

    /** A confirmed order. */
    @Schema(name = "OrderResponse", description = "Order confirmed from a cart")
    public record OrderResponse(

            @Schema(description = "Identifier of the order", example = "58")
            Long id,

            @Schema(description = "Shipping address as a single line",
                    example = "Av. Providencia 1234, Santiago, Region Metropolitana 7500000")
            String shippingAddress,

            @Schema(description = "Order status", example = "CONFIRMED")
            String status,

            @Schema(description = "Sum of every line subtotal", example = "44970")
            BigDecimal total,

            @Schema(description = "Moment the order was confirmed, in UTC")
            Instant createdAt,

            @Schema(description = "Lines of the order")
            List<OrderLineResponse> items) {
    }
}
