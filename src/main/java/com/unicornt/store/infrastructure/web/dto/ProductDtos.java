package com.unicornt.store.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Transport contract of the product resource. Entities never cross the web boundary,
 * so every payload of {@code /api/v1/products} is one of the records below.
 */
public final class ProductDtos {

    private ProductDtos() {
    }

    /** Body of {@code POST /api/v1/products}. */
    @Schema(name = "ProductCreateRequest", description = "New catalog product")
    public record ProductCreateRequest(

            @Schema(description = "Commercial name of the product", example = "Unicorn hoodie",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "name is required")
            @Size(max = 200, message = "name must not exceed 200 characters")
            String name,

            @Schema(description = "Long description shown on the product page",
                    example = "Cotton hoodie with an embroidered unicorn")
            @Size(max = 2000, message = "description must not exceed 2000 characters")
            String description,

            @Schema(description = "Base name of the product image file, without extension",
                    example = "hoodie-unicorn")
            @Size(max = 255, message = "imageBase must not exceed 255 characters")
            String imageBase,

            @Schema(description = "Unit price in whole currency units", example = "25990",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull(message = "price is required")
            @Positive(message = "price must be greater than 0")
            Integer price,

            @Schema(description = "Identifier of an existing category", example = "3",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull(message = "categoryId is required")
            @Positive(message = "categoryId must be greater than 0")
            Integer categoryId,

            @Schema(description = "Identifier of an existing product type", example = "1",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull(message = "productTypeId is required")
            @Positive(message = "productTypeId must be greater than 0")
            Integer productTypeId,

            @Schema(description = "Whether the product is offered in the storefront", example = "true",
                    defaultValue = "true")
            Boolean active) {
    }

    /** Body of {@code PUT /api/v1/products/{id}}: a full replacement of the resource. */
    @Schema(name = "ProductUpdateRequest", description = "Full replacement of an existing product")
    public record ProductUpdateRequest(

            @Schema(description = "Commercial name of the product", example = "Unicorn hoodie",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "name is required")
            @Size(max = 200, message = "name must not exceed 200 characters")
            String name,

            @Schema(description = "Long description shown on the product page",
                    example = "Cotton hoodie with an embroidered unicorn")
            @Size(max = 2000, message = "description must not exceed 2000 characters")
            String description,

            @Schema(description = "Base name of the product image file, without extension",
                    example = "hoodie-unicorn")
            @Size(max = 255, message = "imageBase must not exceed 255 characters")
            String imageBase,

            @Schema(description = "Unit price in whole currency units", example = "25990",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull(message = "price is required")
            @Positive(message = "price must be greater than 0")
            Integer price,

            @Schema(description = "Identifier of an existing category", example = "3",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull(message = "categoryId is required")
            @Positive(message = "categoryId must be greater than 0")
            Integer categoryId,

            @Schema(description = "Identifier of an existing product type", example = "1",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull(message = "productTypeId is required")
            @Positive(message = "productTypeId must be greater than 0")
            Integer productTypeId,

            @Schema(description = "Whether the product is offered in the storefront", example = "true",
                    defaultValue = "true")
            Boolean active) {
    }

    /** Representation returned by every product endpoint. */
    @Schema(name = "ProductResponse", description = "Catalog product as exposed by the API")
    public record ProductResponse(

            @Schema(description = "Product identifier", example = "42")
            int id,

            @Schema(description = "Commercial name of the product", example = "Unicorn hoodie")
            String name,

            @Schema(description = "Long description shown on the product page",
                    example = "Cotton hoodie with an embroidered unicorn")
            String description,

            @Schema(description = "Base name of the product image file, without extension",
                    example = "hoodie-unicorn")
            String imageBase,

            @Schema(description = "Unit price in whole currency units", example = "25990")
            int price,

            @Schema(description = "Identifier of the category the product belongs to", example = "3")
            int categoryId,

            @Schema(description = "Name of the category the product belongs to", example = "Hoodies")
            String categoryName,

            @Schema(description = "Identifier of the product type", example = "1")
            int productTypeId,

            @Schema(description = "Name of the product type", example = "Apparel")
            String productTypeName,

            @Schema(description = "Whether the product is offered in the storefront", example = "true")
            boolean active) {
    }
}
