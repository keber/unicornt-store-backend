package com.unicornt.store.domain.repository;

import com.unicornt.store.domain.model.Category;

import java.util.List;
import java.util.Optional;

/** Port for product-category persistence. Pure domain types in and out. */
public interface CategoryRepository {

    /** Every category, ordered by name. */
    List<Category> findAll();

    Optional<Category> findById(long id);

    boolean existsById(long id);

    Optional<Category> findBySlug(String slug);

    Category save(Category category);
}
