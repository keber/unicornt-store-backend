package com.unicornt.store.infrastructure.persistence.mapper;

import com.unicornt.store.domain.model.Product;
import com.unicornt.store.domain.valueobject.Money;
import com.unicornt.store.infrastructure.persistence.entity.ProductJpaEntity;

/**
 * Converts between the {@link ProductJpaEntity} row and the {@link Product} domain
 * model. Pure static methods, no framework. The category / product-type name labels
 * are read from the entity's transient fields, which the repository adapter fills
 * from a lookup before calling {@link #toDomain}.
 */
public final class ProductPersistenceMapper {

    private ProductPersistenceMapper() {
    }

    public static Product toDomain(ProductJpaEntity entity) {
        return new Product(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getImageBase(),
                Money.ofClp(entity.getPrice()),
                entity.getCategoryId(),
                entity.getCategoryName(),
                entity.getProductTypeId(),
                entity.getProductTypeName(),
                entity.getStock(),
                entity.isActive());
    }

    /** A new entity carrying the column values of {@code product} (id 0 means "insert"). */
    public static ProductJpaEntity toEntity(Product product) {
        ProductJpaEntity entity = new ProductJpaEntity();
        applyColumns(entity, product);
        return entity;
    }

    /** Copies the mutable columns of {@code product} onto an already-loaded {@code entity}. */
    public static void applyColumns(ProductJpaEntity entity, Product product) {
        entity.setId((int) product.id());
        entity.setName(product.name());
        entity.setDescription(product.description());
        entity.setImageBase(product.imageBase());
        entity.setPrice(product.price().amount());
        entity.setCategoryId((int) product.categoryId());
        entity.setProductTypeId((int) product.productTypeId());
        entity.setStock(product.stock());
        entity.setActive(product.isActive());
    }
}
