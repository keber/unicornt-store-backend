package com.unicornt.store.service;

import com.unicornt.store.model.Category;

import java.util.List;

/** Read use cases for product categories. */
public interface CategoryService {

    List<Category> findAll();

    /**
     * Returns the category with the given id.
     *
     * @throws com.unicornt.store.domain.exception.ResourceNotFoundException if it does not exist
     */
    Category findById(int id);
}
