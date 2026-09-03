package com.unicornt.store.application.usecase.catalog;

import com.unicornt.store.domain.exception.DuplicateResourceException;
import com.unicornt.store.domain.model.Category;
import com.unicornt.store.domain.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registers a new category. The domain {@code Category} enforces the name rules and
 * derives the slug; this use case adds the catalog-wide invariant: the slug is unique.
 */
@Service
public class CreateCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public CreateCategoryUseCase(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public Category execute(String name, String slug) {
        Category category = Category.create(name, slug);
        if (categoryRepository.findBySlug(category.slug()).isPresent()) {
            throw new DuplicateResourceException("Category", "slug", category.slug());
        }
        return categoryRepository.save(category);
    }
}
