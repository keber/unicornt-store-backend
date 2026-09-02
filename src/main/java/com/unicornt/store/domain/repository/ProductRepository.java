package com.unicornt.store.domain.repository;

import com.unicornt.store.domain.model.Product;

import java.util.Optional;

/**
 * Port for catalog product persistence. Pure domain types in and out; the JPA
 * adapter in {@code infrastructure.persistence.adapter} implements it.
 */
public interface ProductRepository {

    /**
     * Paginated catalog search. Both filters are optional: a {@code null} or blank
     * value disables that filter.
     *
     * @param categoryFilter category slug or category name, matched case-insensitively
     * @param textFilter     free text matched against the product name and description
     * @param page           zero-based page number
     * @param size           page size (must be positive)
     */
    PageResult<Product> search(String categoryFilter, String textFilter, int page, int size);

    Optional<Product> findById(long id);

    boolean existsById(long id);

    Product save(Product product);

    void deleteById(long id);
}
