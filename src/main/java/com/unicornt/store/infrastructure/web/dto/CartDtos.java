package com.unicornt.store.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

/** Request and response payloads of the cart resource. */
public final class CartDtos {

    private CartDtos() {
    }

    /** Body of {@code POST /api/v1/cart/items}. */
    @Schema(name = "CartItemRequest", description = "Product and quantity to add to the cart")
    public record CartItemRequest(

            @Schema(description = "Identifier of the product to add", example = "12")
            @NotNull(message = "productId is required")
            @Positive(message = "productId must be greater than 0")
            Integer productId,

            @Schema(description = "Units to add", example = "2")
            @NotNull(message = "qty is required")
            @Positive(message = "qty must be greater than 0")
            Integer qty) {
    }

    /** Body of {@code PATCH /api/v1/cart/items/{id}}. */
    @Schema(name = "CartItemQuantityRequest", description = "New quantity for an existing cart line")
    public record CartItemQuantityRequest(

            @Schema(description = "New number of units of the line", example = "3")
            @NotNull(message = "qty is required")
            @Positive(message = "qty must be greater than 0")
            Integer qty) {
    }

    /** One priced line of the cart. */
    @Schema(name = "CartItemResponse", description = "A single priced cart line")
    public record CartItemResponse(

            @Schema(description = "Identifier of the cart line", example = "45")
            Long id,

            @Schema(description = "Identifier of the product", example = "12")
            int productId,

            @Schema(description = "Product name at the time of reading the cart", example = "Unicorn plush")
            String productName,

            @Schema(description = "Base name of the product image", example = "unicorn-plush")
            String imageBase,

            @Schema(description = "Unit price of the product", example = "14990")
            BigDecimal unitPrice,

            @Schema(description = "Units of this line", example = "2")
            int quantity,

            @Schema(description = "Unit price multiplied by the quantity", example = "29980")
            BigDecimal subtotal) {
    }

    /** The whole cart of the authenticated user. */
    @Schema(name = "CartResponse", description = "Cart of the authenticated user")
    public record CartResponse(

            @Schema(description = "Lines of the cart")
            List<CartItemResponse> items,

            @Schema(description = "Total number of units across every line", example = "3")
            int itemCount,

            @Schema(description = "Sum of every line subtotal", example = "44970")
            BigDecimal total) {
    }
}
