package com.unicornt.store.infrastructure.web.mapper;

import com.unicornt.store.infrastructure.persistence.entity.CategoryEntity;
import com.unicornt.store.infrastructure.web.dto.CategoryDtos.CategoryCreateRequest;
import com.unicornt.store.infrastructure.web.dto.CategoryDtos.CategoryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit test of the pure static translation between category records and the category entity. */
class CategoryMapperTest {

    @Nested
    @DisplayName("toResponse")
    class ToResponse {

        @Test
        @DisplayName("copies id, name and slug onto the response record")
        void mapsAllFields() {
            CategoryEntity entity = new CategoryEntity();
            entity.setId(3);
            entity.setName("Hoodies");
            entity.setSlug("hoodies");

            CategoryResponse response = CategoryMapper.toResponse(entity);

            assertThat(response.id()).isEqualTo(3);
            assertThat(response.name()).isEqualTo("Hoodies");
            assertThat(response.slug()).isEqualTo("hoodies");
        }

        @Test
        @DisplayName("renders an unset slug as the empty string the entity exposes")
        void unsetSlug() {
            CategoryEntity entity = new CategoryEntity();
            entity.setName("Hoodies");

            assertThat(CategoryMapper.toResponse(entity).slug()).isEmpty();
        }
    }

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("trims the name and passes the slug through unchanged")
        void trimsNameKeepsSlug() {
            CategoryEntity entity = CategoryMapper.toEntity(
                    new CategoryCreateRequest("  Hoodies  ", "hoodies"));

            assertThat(entity.getName()).isEqualTo("Hoodies");
            assertThat(entity.getSlug()).isEqualTo("hoodies");
        }

        @Test
        @DisplayName("keeps a null name as null")
        void nullName() {
            CategoryEntity entity = CategoryMapper.toEntity(
                    new CategoryCreateRequest(null, "hoodies"));

            assertThat(entity.getName()).isNull();
        }

        @Test
        @DisplayName("passes a null slug through as the empty string the entity exposes")
        void nullSlug() {
            CategoryEntity entity = CategoryMapper.toEntity(
                    new CategoryCreateRequest("Hoodies", null));

            assertThat(entity.getSlug()).isEmpty();
        }
    }
}
