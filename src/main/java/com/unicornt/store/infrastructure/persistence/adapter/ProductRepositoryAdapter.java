package com.unicornt.store.infrastructure.persistence.adapter;

import com.unicornt.store.domain.model.Product;
import com.unicornt.store.domain.repository.PageResult;
import com.unicornt.store.domain.repository.ProductRepository;
import com.unicornt.store.infrastructure.persistence.entity.CategoryJpaEntity;
import com.unicornt.store.infrastructure.persistence.entity.ProductJpaEntity;
import com.unicornt.store.infrastructure.persistence.entity.ProductTypeJpaEntity;
import com.unicornt.store.infrastructure.persistence.mapper.ProductPersistenceMapper;
import com.unicornt.store.infrastructure.persistence.repository.SpringDataCategoryRepository;
import com.unicornt.store.infrastructure.persistence.repository.SpringDataProductRepository;
import com.unicornt.store.infrastructure.persistence.repository.SpringDataProductTypeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** JPA-backed implementation of the {@link ProductRepository} port. */
@Component
public class ProductRepositoryAdapter implements ProductRepository {

    private static final Sort BY_ID = Sort.by(Sort.Direction.ASC, "id");

    private final SpringDataProductRepository products;
    private final SpringDataCategoryRepository categories;
    private final SpringDataProductTypeRepository productTypes;

    public ProductRepositoryAdapter(SpringDataProductRepository products,
                                    SpringDataCategoryRepository categories,
                                    SpringDataProductTypeRepository productTypes) {
        this.products = products;
        this.categories = categories;
        this.productTypes = productTypes;
    }

    @Override
    public PageResult<Product> search(String categoryFilter, String textFilter, int page, int size) {
        Page<ProductJpaEntity> result = products.search(
                nullToEmpty(categoryFilter), nullToEmpty(textFilter),
                PageRequest.of(page, size, BY_ID));
        List<Product> content = toDomainList(result.getContent());
        return new PageResult<>(content, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Override
    public Optional<Product> findById(long id) {
        return products.findById((int) id).map(entity -> toDomainList(List.of(entity)).get(0));
    }

    @Override
    public boolean existsById(long id) {
        return products.existsById((int) id);
    }

    @Override
    public Product save(Product product) {
        ProductJpaEntity saved = products.save(ProductPersistenceMapper.toEntity(product));
        return toDomainList(List.of(saved)).get(0);
    }

    @Override
    public void deleteById(long id) {
        products.deleteById((int) id);
    }

    /** Fills the transient category and product-type name labels, then maps to the domain model. */
    private List<Product> toDomainList(List<ProductJpaEntity> entities) {
        if (entities.isEmpty()) {
            return List.of();
        }
        Map<Integer, String> categoryNames = categories.findAll().stream()
                .collect(Collectors.toMap(CategoryJpaEntity::getId, CategoryJpaEntity::getName, keepFirst()));
        Map<Integer, String> typeNames = productTypes.findAll().stream()
                .collect(Collectors.toMap(ProductTypeJpaEntity::getId, ProductTypeJpaEntity::getName, keepFirst()));
        return entities.stream().map(entity -> {
            entity.setCategoryName(categoryNames.get(entity.getCategoryId()));
            entity.setProductTypeName(typeNames.get(entity.getProductTypeId()));
            return ProductPersistenceMapper.toDomain(entity);
        }).toList();
    }

    private static <T> java.util.function.BinaryOperator<T> keepFirst() {
        return (first, second) -> first;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
