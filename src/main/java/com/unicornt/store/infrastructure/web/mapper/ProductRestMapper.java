package com.unicornt.store.infrastructure.web.mapper;

import com.unicornt.store.application.usecase.catalog.ProductCommand;
import com.unicornt.store.domain.model.Product;
import com.unicornt.store.domain.repository.PageResult;
import com.unicornt.store.infrastructure.web.dto.ProductDtos.ProductCreateRequest;
import com.unicornt.store.infrastructure.web.dto.ProductDtos.ProductPageResponse;
import com.unicornt.store.infrastructure.web.dto.ProductDtos.ProductResponse;
import com.unicornt.store.infrastructure.web.dto.ProductDtos.ProductUpdateRequest;

/** Translation between the product transport records and the domain model / use-case input. */
public final class ProductRestMapper {

    private ProductRestMapper() {
    }

    public static ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.id(),
                product.name(),
                product.description(),
                product.imageBase(),
                product.price().amount(),
                product.categoryId(),
                product.categoryName(),
                product.productTypeId(),
                product.productTypeName(),
                product.stock(),
                product.isActive());
    }

    public static ProductPageResponse toPageResponse(PageResult<Product> page) {
        return new ProductPageResponse(
                page.content().stream().map(ProductRestMapper::toResponse).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }

    public static ProductCommand toCommand(ProductCreateRequest request) {
        return new ProductCommand(
                request.name(), request.description(), request.imageBase(),
                request.price(), request.categoryId(), request.productTypeId(),
                request.stock(), request.active() == null || request.active());
    }

    public static ProductCommand toCommand(ProductUpdateRequest request) {
        return new ProductCommand(
                request.name(), request.description(), request.imageBase(),
                request.price(), request.categoryId(), request.productTypeId(),
                request.stock(), request.active() == null || request.active());
    }
}
