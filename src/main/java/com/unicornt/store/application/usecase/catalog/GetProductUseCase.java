package com.unicornt.store.application.usecase.catalog;

import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.domain.model.Product;
import com.unicornt.store.domain.repository.ProductRepository;
import org.springframework.stereotype.Service;

/** Returns a single product by id, or fails with a not-found. */
@Service
public class GetProductUseCase {

    private final ProductRepository productRepository;

    public GetProductUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product execute(long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }
}
