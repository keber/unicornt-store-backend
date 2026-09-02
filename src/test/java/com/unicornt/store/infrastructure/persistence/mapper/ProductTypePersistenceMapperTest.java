package com.unicornt.store.infrastructure.persistence.mapper;

import com.unicornt.store.domain.model.ProductType;
import com.unicornt.store.infrastructure.persistence.entity.ProductTypeJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProductTypePersistenceMapper")
class ProductTypePersistenceMapperTest {

    @Test
    @DisplayName("maps a row to the domain model")
    void toDomain() {
        ProductTypeJpaEntity entity = new ProductTypeJpaEntity();
        entity.setId(1);
        entity.setName("T-shirt");
        entity.setSlug("t-shirt");

        ProductType type = ProductTypePersistenceMapper.toDomain(entity);

        assertThat(type.id()).isEqualTo(1L);
        assertThat(type.name()).isEqualTo("T-shirt");
        assertThat(type.slug()).isEqualTo("t-shirt");
    }
}
