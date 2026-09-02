package com.unicornt.store.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

/**
 * Transport contract of the cart resource. Domain models never cross the web
 * boundary, so every payload of {@code /api/v1/cart} is one of the records below.
 * Quantities are named {@code quantity} everywhere (never {@code qty}); money is a
 * whole-CLP integer.
 */
public final class CartDtos {

    private CartDtos() {
    }

    /** Body of {@code POST /api/v1/cart/items}. */
    @Schema(name = "AddCartItemRequest", description = "Product and units to add to the cart")
    public record AddCartItemRequest(

            @Schema(description = "Identifier of the product to add", example = "12",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull(message = "productId is required")
            @Positive(message = "productId must be greater than 0")
            Long productId,

            @Schema(description = "Units to add", example = "2",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull(message = "quantity is required")
            @Positive(message = "quantity must be greater than 0")
            Integer quantity) {
    }

    /** Body of {@code PUT /api/v1/cart/items/{productId}}. */
    @Schema(name = "UpdateCartItemRequest", description = "New absolute quantity for an existing cart line")
    public record UpdateCartItemRequest(

            @Schema(description = "New number of units of the line; 0 removes it", example = "3",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull(message = "quantity is required")
            @PositiveOrZero(message = "quantity must not be negative")
            Integer quantity) {
    }

    /** Body of {@code POST /api/v1/cart/merge}: the client's local cart. */
    @Schema(name = "MergeCartRequest", description = "Local (anonymous) cart lines to fold into the user's cart")
    public record MergeCartRequest(

            @Schema(description = "Local cart lines", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull(message = "items is required")
            @NotEmpty(message = "items must not be empty")
            List<@Valid MergeCartItem> items) {
    }

    /** One line of the local cart being merged. */
    @Schema(name = "MergeCartItem", description = "A single local cart line")
    public record MergeCartItem(

            @Schema(description = "Identifier of the product", example = "12",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull(message = "productId is required")
            @Positive(message = "productId must be greater than 0")
            Long productId,

            @Schema(description = "Units held locally", example = "1",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull(message = "quantity is required")
            @Positive(message = "quantity must be greater than 0")
            Integer quantity) {
    }

    /** One priced line of the cart. */
    @Schema(name = "CartItemResponse", description = "A single priced cart line")
    public record CartItemResponse(

            @Schema(description = "Identifier of the product", example = "12")
            long productId,

            @Schema(description = "Product name at the time the cart was read", example = "Unicorn plush")
            String productName,

            @Schema(description = "Base name of the product image file, without extension",
                    example = "unicorn-plush")
            String imageBase,

            @Schema(description = "Unit price in whole CLP units", example = "14990")
            int unitPrice,

            @Schema(description = "Units of this line", example = "2")
            int quantity,

            @Schema(description = "Unit price multiplied by the quantity", example = "29980")
            int subtotal) {
    }

    /** The whole cart of the authenticated user. */
    @Schema(name = "CartResponse", description = "Cart of the authenticated user")
    public record CartResponse(

            @Schema(description = "Lines of the cart")
            List<CartItemResponse> items,

            @Schema(description = "Total number of units across every line", example = "3")
            int itemCount,

            @Schema(description = "Sum of every line subtotal, in whole CLP units", example = "44970")
            int total) {
    }
}
