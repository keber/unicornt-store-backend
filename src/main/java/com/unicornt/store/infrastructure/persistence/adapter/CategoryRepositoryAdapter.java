package com.unicornt.store.infrastructure.persistence.adapter;

import com.unicornt.store.domain.model.Category;
import com.unicornt.store.domain.repository.CategoryRepository;
import com.unicornt.store.infrastructure.persistence.mapper.CategoryPersistenceMapper;
import com.unicornt.store.infrastructure.persistence.repository.SpringDataCategoryRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** JPA-backed implementation of the {@link CategoryRepository} port. */
@Component
public class CategoryRepositoryAdapter implements CategoryRepository {

    private final SpringDataCategoryRepository categories;

    public CategoryRepositoryAdapter(SpringDataCategoryRepository categories) {
        this.categories = categories;
    }

    @Override
    public List<Category> findAll() {
        return categories.findAllByOrderByNameAsc().stream()
                .map(CategoryPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Category> findById(long id) {
        return categories.findById((int) id).map(CategoryPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsById(long id) {
        return categories.existsById((int) id);
    }

    @Override
    public Optional<Category> findBySlug(String slug) {
        return categories.findBySlug(slug).map(CategoryPersistenceMapper::toDomain);
    }

    @Override
    public Category save(Category category) {
        return CategoryPersistenceMapper.toDomain(
                categories.save(CategoryPersistenceMapper.toEntity(category)));
    }
}
