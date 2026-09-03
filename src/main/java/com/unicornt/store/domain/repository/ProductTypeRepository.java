package com.unicornt.store.domain.repository;

import com.unicornt.store.domain.model.ProductType;

import java.util.List;
import java.util.Optional;

/** Port for product-type reference data. Pure domain types in and out. */
public interface ProductTypeRepository {

    List<ProductType> findAll();

    Optional<ProductType> findById(long id);

    boolean existsById(long id);
}
