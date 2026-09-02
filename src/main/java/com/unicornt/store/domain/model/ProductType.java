package com.unicornt.store.domain.model;

/**
 * The kind of item a product is (T-shirt, Mug, Poster). A small reference value:
 * the catalog only needs its id and name to validate a product and to label it.
 */
public record ProductType(long id, String name, String slug) {

    public ProductType {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("product type name is required");
        }
    }
}
