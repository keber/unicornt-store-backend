package com.unicornt.store.infrastructure.persistence.mapper;

import com.unicornt.store.domain.model.ProductType;
import com.unicornt.store.infrastructure.persistence.entity.ProductTypeJpaEntity;

/** Converts a {@link ProductTypeJpaEntity} row into the {@link ProductType} domain model. */
public final class ProductTypePersistenceMapper {

    private ProductTypePersistenceMapper() {
    }

    public static ProductType toDomain(ProductTypeJpaEntity entity) {
        return new ProductType(entity.getId(), entity.getName(), entity.getSlug());
    }
}
