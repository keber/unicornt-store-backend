package com.unicornt.store.application.usecase.catalog;

import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.domain.model.Product;
import com.unicornt.store.domain.repository.CategoryRepository;
import com.unicornt.store.domain.repository.ProductRepository;
import com.unicornt.store.domain.repository.ProductTypeRepository;
import com.unicornt.store.domain.valueobject.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registers a new catalog product. The full validation walk: the domain
 * {@code Product} constructor enforces name / price / stock, then the referenced
 * category and product type must exist.
 */
@Service
public class CreateProductUseCase {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductTypeRepository productTypeRepository;

    public CreateProductUseCase(ProductRepository productRepository,
                                CategoryRepository categoryRepository,
                                ProductTypeRepository productTypeRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productTypeRepository = productTypeRepository;
    }

    @Transactional
    public Product execute(ProductCommand command) {
        Product product = Product.create(
                command.name(), command.description(), command.imageBase(),
                Money.ofClp(command.priceClp()),
                command.categoryId(), command.productTypeId(),
                command.stock(), command.active());
        requireReferences(command);
        return productRepository.save(product);
    }

    private void requireReferences(ProductCommand command) {
        if (!categoryRepository.existsById(command.categoryId())) {
            throw new ResourceNotFoundException("Category", command.categoryId());
        }
        if (!productTypeRepository.existsById(command.productTypeId())) {
            throw new ResourceNotFoundException("Product type", command.productTypeId());
        }
    }
}
