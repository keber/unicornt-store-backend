package com.unicornt.store.infrastructure.persistence.mapper;

import com.unicornt.store.domain.model.Category;
import com.unicornt.store.infrastructure.persistence.entity.CategoryJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CategoryPersistenceMapper")
class CategoryPersistenceMapperTest {

    @Test
    @DisplayName("maps a row to the domain model")
    void toDomain() {
        CategoryJpaEntity entity = new CategoryJpaEntity();
        entity.setId(3);
        entity.setName("Unicorns");
        entity.setSlug("unicorns");

        Category category = CategoryPersistenceMapper.toDomain(entity);

        assertThat(category.id()).isEqualTo(3L);
        assertThat(category.name()).isEqualTo("Unicorns");
        assertThat(category.slug()).isEqualTo("unicorns");
    }

    @Test
    @DisplayName("maps the domain model back to a row")
    void toEntity() {
        CategoryJpaEntity entity = CategoryPersistenceMapper.toEntity(new Category(5L, "Stars", "stars"));

        assertThat(entity.getId()).isEqualTo(5);
        assertThat(entity.getName()).isEqualTo("Stars");
        assertThat(entity.getSlug()).isEqualTo("stars");
    }
}
