package com.unicornt.store.infrastructure.web.mapper;

import com.unicornt.store.infrastructure.persistence.entity.CategoryEntity;
import com.unicornt.store.infrastructure.web.dto.CategoryDtos.CategoryCreateRequest;
import com.unicornt.store.infrastructure.web.dto.CategoryDtos.CategoryResponse;

/** Translation between the category transport records and the persistence entity. */
public final class CategoryMapper {

    private CategoryMapper() {
    }

    public static CategoryResponse toResponse(CategoryEntity entity) {
        return new CategoryResponse(entity.getId(), entity.getName(), entity.getSlug());
    }

    public static CategoryEntity toEntity(CategoryCreateRequest request) {
        CategoryEntity entity = new CategoryEntity();
        entity.setName(request.name() != null ? request.name().trim() : null);
        entity.setSlug(request.slug());
        return entity;
    }
}
