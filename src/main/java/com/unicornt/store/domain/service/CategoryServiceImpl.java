package com.unicornt.store.domain.service;

import com.unicornt.store.domain.exception.DuplicateResourceException;
import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.infrastructure.persistence.entity.CategoryEntity;
import com.unicornt.store.infrastructure.persistence.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
public class CategoryServiceImpl implements CategoryService {

    private static final int MAX_NAME_LENGTH = 100;

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<CategoryEntity> findAll() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    @Override
    public CategoryEntity findById(int id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    @Override
    @Transactional
    public CategoryEntity create(CategoryEntity category) {
        String name = category.getName();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name is required");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Category name must not exceed " + MAX_NAME_LENGTH + " characters");
        }
        String slug = category.getSlug().isBlank() ? slugify(name) : slugify(category.getSlug());
        if (categoryRepository.findBySlug(slug).isPresent()) {
            throw new DuplicateResourceException("Category", "slug", slug);
        }
        category.setId(0);
        category.setName(name.trim());
        category.setSlug(slug);
        return categoryRepository.save(category);
    }

    /** Builds a URL friendly identifier: lower case, accent free, hyphen separated. */
    private String slugify(String value) {
        // NFD splits accented letters into a base letter plus a combining mark; the
        // alphanumeric filter below then drops the marks.
        String ascii = Normalizer.normalize(value, Normalizer.Form.NFD);
        String slug = ascii.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (slug.isBlank()) {
            throw new IllegalArgumentException("Category name must contain at least one alphanumeric character");
        }
        return slug;
    }
}
