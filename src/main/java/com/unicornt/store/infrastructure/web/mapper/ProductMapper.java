package com.unicornt.store.infrastructure.web.mapper;

import com.unicornt.store.infrastructure.persistence.entity.ProductEntity;
import com.unicornt.store.infrastructure.web.dto.ProductDtos.ProductCreateRequest;
import com.unicornt.store.infrastructure.web.dto.ProductDtos.ProductResponse;
import com.unicornt.store.infrastructure.web.dto.ProductDtos.ProductUpdateRequest;

/** Translation between the product transport records and the persistence entity. */
public final class ProductMapper {

    private ProductMapper() {
    }

    public static ProductResponse toResponse(ProductEntity entity) {
        return new ProductResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getImageBase(),
                entity.getPrice(),
                entity.getCategoryId(),
                entity.getCategoryName(),
                entity.getProductTypeId(),
                entity.getProductTypeName(),
                entity.isActive());
    }

    public static ProductEntity toEntity(ProductCreateRequest request) {
        return fill(new ProductEntity(), request.name(), request.description(), request.imageBase(),
                request.price(), request.categoryId(), request.productTypeId(), request.active());
    }

    public static ProductEntity toEntity(ProductUpdateRequest request) {
        return fill(new ProductEntity(), request.name(), request.description(), request.imageBase(),
                request.price(), request.categoryId(), request.productTypeId(), request.active());
    }

    private static ProductEntity fill(ProductEntity entity, String name, String description,
                                      String imageBase, Integer price, Integer categoryId,
                                      Integer productTypeId, Boolean active) {
        entity.setName(name != null ? name.trim() : null);
        entity.setDescription(description);
        entity.setImageBase(imageBase);
        entity.setPrice(price != null ? price : 0);
        entity.setCategoryId(categoryId != null ? categoryId : 0);
        entity.setProductTypeId(productTypeId != null ? productTypeId : 0);
        entity.setActive(active == null || active);
        return entity;
    }
}
