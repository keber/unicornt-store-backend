package com.unicornt.store.infrastructure.web.mapper;

import com.unicornt.store.domain.model.Category;
import com.unicornt.store.infrastructure.web.dto.CategoryDtos.CategoryResponse;

/** Translation between the category transport records and the domain model. */
public final class CategoryRestMapper {

    private CategoryRestMapper() {
    }

    public static CategoryResponse toResponse(Category category) {
        return new CategoryResponse((int) category.id(), category.name(), category.slug());
    }
}
