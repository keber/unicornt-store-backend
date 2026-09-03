package com.unicornt.store.application.usecase.catalog;

import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.domain.model.Product;
import com.unicornt.store.domain.repository.CategoryRepository;
import com.unicornt.store.domain.repository.ProductRepository;
import com.unicornt.store.domain.repository.ProductTypeRepository;
import com.unicornt.store.domain.valueobject.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Full replacement of an existing product; same validation walk as create. */
@Service
public class UpdateProductUseCase {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductTypeRepository productTypeRepository;

    public UpdateProductUseCase(ProductRepository productRepository,
                                CategoryRepository categoryRepository,
                                ProductTypeRepository productTypeRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productTypeRepository = productTypeRepository;
    }

    @Transactional
    public Product execute(long id, ProductCommand command) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product", id);
        }
        Product replacement = new Product(
                id, command.name(), command.description(), command.imageBase(),
                Money.ofClp(command.priceClp()),
                command.categoryId(), null, command.productTypeId(), null,
                command.stock(), command.active());
        if (!categoryRepository.existsById(command.categoryId())) {
            throw new ResourceNotFoundException("Category", command.categoryId());
        }
        if (!productTypeRepository.existsById(command.productTypeId())) {
            throw new ResourceNotFoundException("Product type", command.productTypeId());
        }
        return productRepository.save(replacement);
    }
}
