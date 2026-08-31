package com.unicornt.store.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Transport contract of the category resource. */
public final class CategoryDtos {

    private CategoryDtos() {
    }

    /** Body of {@code POST /api/v1/categories}. */
    @Schema(name = "CategoryCreateRequest", description = "New product category")
    public record CategoryCreateRequest(

            @Schema(description = "Display name of the category", example = "Hoodies",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "name is required")
            @Size(max = 100, message = "name must not exceed 100 characters")
            String name,

            @Schema(description = "URL friendly identifier; derived from the name when omitted",
                    example = "hoodies")
            @Size(max = 100, message = "slug must not exceed 100 characters")
            @Pattern(regexp = "^[a-zA-Z0-9]+(?:-[a-zA-Z0-9]+)*$",
                    message = "slug must contain only alphanumeric characters separated by hyphens")
            String slug) {
    }

    /** Representation returned by every category endpoint. */
    @Schema(name = "CategoryResponse", description = "Product category as exposed by the API")
    public record CategoryResponse(

            @Schema(description = "Category identifier", example = "3")
            int id,

            @Schema(description = "Display name of the category", example = "Hoodies")
            String name,

            @Schema(description = "URL friendly identifier", example = "hoodies")
            String slug) {
    }
}
