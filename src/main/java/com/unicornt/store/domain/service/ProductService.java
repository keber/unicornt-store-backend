package com.unicornt.store.domain.service;

import com.unicornt.store.infrastructure.persistence.entity.ProductEntity;
import com.unicornt.store.infrastructure.persistence.entity.ProductTypeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/** Catalog product use cases, independent of any transport technology. */
public interface ProductService {

    List<ProductEntity> findAll(String nameFilter, Integer categoryId);

    List<ProductEntity> findAll(String nameFilter, Integer categoryId, int limit, int offset);

    long countAll(String nameFilter, Integer categoryId);

    /**
     * Paginated catalog search. Both filters are optional: a null or blank value disables it.
     *
     * @param category category slug or category name, matched case insensitively
     * @param q        free text matched against the product name and description
     * @param pageable page number, page size and sort order
     */
    Page<ProductEntity> search(String category, String q, Pageable pageable);

    /**
     * Returns the product with the given id.
     *
     * @throws com.unicornt.store.domain.exception.ResourceNotFoundException if it does not exist
     */
    ProductEntity findById(int id);

    ProductEntity create(ProductEntity product);

    ProductEntity update(int id, ProductEntity product);

    void delete(int id);

    List<ProductTypeEntity> findAllProductTypes();
}
