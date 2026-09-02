package com.unicornt.store.infrastructure.persistence.adapter;

import com.unicornt.store.domain.model.ProductType;
import com.unicornt.store.domain.repository.ProductTypeRepository;
import com.unicornt.store.infrastructure.persistence.mapper.ProductTypePersistenceMapper;
import com.unicornt.store.infrastructure.persistence.repository.SpringDataProductTypeRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** JPA-backed implementation of the {@link ProductTypeRepository} port. */
@Component
public class ProductTypeRepositoryAdapter implements ProductTypeRepository {

    private final SpringDataProductTypeRepository productTypes;

    public ProductTypeRepositoryAdapter(SpringDataProductTypeRepository productTypes) {
        this.productTypes = productTypes;
    }

    @Override
    public List<ProductType> findAll() {
        return productTypes.findAllByOrderByIdAsc().stream()
                .map(ProductTypePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<ProductType> findById(long id) {
        return productTypes.findById((int) id).map(ProductTypePersistenceMapper::toDomain);
    }

    @Override
    public boolean existsById(long id) {
        return productTypes.existsById((int) id);
    }
}
