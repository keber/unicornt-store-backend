package com.unicornt.store.application.usecase.catalog;

/**
 * Input for creating or replacing a catalog product. Carries raw values from the
 * transport layer; the domain {@code Product} constructor validates them.
 */
public record ProductCommand(
        String name,
        String description,
        String imageBase,
        int priceClp,
        long categoryId,
        long productTypeId,
        int stock,
        boolean active) {
}
