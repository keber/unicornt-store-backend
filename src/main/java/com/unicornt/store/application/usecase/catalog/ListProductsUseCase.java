package com.unicornt.store.application.usecase.catalog;

import com.unicornt.store.domain.model.Product;
import com.unicornt.store.domain.repository.PageResult;
import com.unicornt.store.domain.repository.ProductRepository;
import org.springframework.stereotype.Service;

/** Lists the catalog, optionally filtered by category and free text, one page at a time. */
@Service
public class ListProductsUseCase {

    /** Upper bound so a caller cannot ask for an unbounded page. */
    static final int MAX_PAGE_SIZE = 100;
    static final int DEFAULT_PAGE_SIZE = 20;

    private final ProductRepository productRepository;

    public ListProductsUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public PageResult<Product> execute(String category, String text, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        return productRepository.search(blankToNull(category), blankToNull(text), safePage, safeSize);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
