package com.unicornt.store.domain.service;

import com.unicornt.store.infrastructure.persistence.entity.CategoryEntity;

import java.util.List;

/** Use cases for product categories. */
public interface CategoryService {

    List<CategoryEntity> findAll();

    /**
     * Returns the category with the given id.
     *
     * @throws com.unicornt.store.domain.exception.ResourceNotFoundException if it does not exist
     */
    CategoryEntity findById(int id);

    /**
     * Registers a new category. The slug is derived from the name when it is not supplied.
     *
     * @throws IllegalArgumentException if the name is missing or too long
     * @throws com.unicornt.store.domain.exception.DuplicateResourceException if the slug is taken
     */
    CategoryEntity create(CategoryEntity category);
}
