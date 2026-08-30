package com.unicornt.store.domain.service;

import com.unicornt.store.infrastructure.persistence.entity.CategoryEntity;

import java.util.List;

/** Read use cases for product categories. */
public interface CategoryService {

    List<CategoryEntity> findAll();

    /**
     * Returns the category with the given id.
     *
     * @throws com.unicornt.store.domain.exception.ResourceNotFoundException if it does not exist
     */
    CategoryEntity findById(int id);
}
