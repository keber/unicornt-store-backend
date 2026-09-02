package com.unicornt.store.infrastructure.persistence.mapper;

import com.unicornt.store.domain.model.Category;
import com.unicornt.store.infrastructure.persistence.entity.CategoryJpaEntity;

/** Converts between the {@link CategoryJpaEntity} row and the {@link Category} domain model. */
public final class CategoryPersistenceMapper {

    private CategoryPersistenceMapper() {
    }

    public static Category toDomain(CategoryJpaEntity entity) {
        return new Category(entity.getId(), entity.getName(), entity.getSlug());
    }

    public static CategoryJpaEntity toEntity(Category category) {
        CategoryJpaEntity entity = new CategoryJpaEntity();
        entity.setId((int) category.id());
        entity.setName(category.name());
        entity.setSlug(category.slug());
        return entity;
    }
}
