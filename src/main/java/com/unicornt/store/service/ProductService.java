package com.unicornt.store.service;

import com.unicornt.store.model.Product;
import com.unicornt.store.model.ProductType;

import java.util.List;

/** Catalog product use cases, independent of any transport technology. */
public interface ProductService {

    List<Product> findAll(String nameFilter, Integer categoryId);

    List<Product> findAll(String nameFilter, Integer categoryId, int limit, int offset);

    long countAll(String nameFilter, Integer categoryId);

    /**
     * Returns the product with the given id.
     *
     * @throws com.unicornt.store.domain.exception.ResourceNotFoundException if it does not exist
     */
    Product findById(int id);

    Product create(Product product);

    Product update(int id, Product product);

    void delete(int id);

    List<ProductType> findAllProductTypes();
}
