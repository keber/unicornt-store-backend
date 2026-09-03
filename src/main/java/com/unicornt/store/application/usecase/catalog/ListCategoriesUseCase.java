package com.unicornt.store.application.usecase.catalog;

import com.unicornt.store.domain.model.Category;
import com.unicornt.store.domain.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/** Returns every product category for the storefront filter. */
@Service
public class ListCategoriesUseCase {

    private final CategoryRepository categoryRepository;

    public ListCategoriesUseCase(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> execute() {
        return categoryRepository.findAll();
    }
}
