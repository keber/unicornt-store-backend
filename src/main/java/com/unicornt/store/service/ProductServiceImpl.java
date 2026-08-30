package com.unicornt.store.service;

import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.model.Category;
import com.unicornt.store.model.Product;
import com.unicornt.store.model.ProductType;
import com.unicornt.store.repository.CategoryRepository;
import com.unicornt.store.repository.ProductRepository;
import com.unicornt.store.repository.ProductTypeRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private static final Sort BY_ID = Sort.by(Sort.Direction.ASC, "id");
    private static final int MAX_NAME_LENGTH = 200;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductTypeRepository productTypeRepository;

    public ProductServiceImpl(ProductRepository productRepository,
                              CategoryRepository categoryRepository,
                              ProductTypeRepository productTypeRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productTypeRepository = productTypeRepository;
    }

    @Override
    public List<Product> findAll(String nameFilter, Integer categoryId) {
        String name = normalize(nameFilter);
        Integer category = normalizeCategory(categoryId);
        List<Product> products = category == null
                ? productRepository.findByNameContainingIgnoreCase(name, BY_ID)
                : productRepository.findByNameContainingIgnoreCaseAndCategoryId(name, category, BY_ID);
        return enrich(products);
    }

    @Override
    public List<Product> findAll(String nameFilter, Integer categoryId, int limit, int offset) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be greater than 0");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        String name = normalize(nameFilter);
        Integer category = normalizeCategory(categoryId);
        PageRequest page = PageRequest.of(offset / limit, limit, BY_ID);
        List<Product> products = category == null
                ? productRepository.findByNameContainingIgnoreCase(name, page).getContent()
                : productRepository.findByNameContainingIgnoreCaseAndCategoryId(name, category, page).getContent();
        return enrich(products);
    }

    @Override
    public long countAll(String nameFilter, Integer categoryId) {
        String name = normalize(nameFilter);
        Integer category = normalizeCategory(categoryId);
        return category == null
                ? productRepository.countByNameContainingIgnoreCase(name)
                : productRepository.countByNameContainingIgnoreCaseAndCategoryId(name, category);
    }

    @Override
    public Product findById(int id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        return enrich(List.of(product)).get(0);
    }

    @Override
    @Transactional
    public Product create(Product product) {
        validate(product);
        product.setId(0);
        return enrich(List.of(productRepository.save(product))).get(0);
    }

    @Override
    @Transactional
    public Product update(int id, Product product) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        validate(product);
        existing.setName(product.getName());
        existing.setDescription(product.getDescription());
        existing.setImageBase(product.getImageBase());
        existing.setPrice(product.getPrice());
        existing.setCategoryId(product.getCategoryId());
        existing.setProductTypeId(product.getProductTypeId());
        existing.setActive(product.isActive());
        return enrich(List.of(productRepository.save(existing))).get(0);
    }

    @Override
    @Transactional
    public void delete(int id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product", id);
        }
        productRepository.deleteById(id);
    }

    @Override
    public List<ProductType> findAllProductTypes() {
        return productTypeRepository.findAllByOrderByIdAsc();
    }

    // ----------------------------------------------------------------
    // Business rules preserved from the removed admin controller
    // ----------------------------------------------------------------

    private void validate(Product product) {
        String name = product.getName();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Product name must not exceed " + MAX_NAME_LENGTH + " characters");
        }
        if (product.getPrice() <= 0) {
            throw new IllegalArgumentException("Price must be an integer greater than 0");
        }
        if (product.getCategoryId() <= 0) {
            throw new IllegalArgumentException("A category must be selected");
        }
        if (!categoryRepository.existsById(product.getCategoryId())) {
            throw new ResourceNotFoundException("Category", product.getCategoryId());
        }
        if (product.getProductTypeId() <= 0) {
            throw new IllegalArgumentException("A product type must be selected");
        }
        if (!productTypeRepository.existsById(product.getProductTypeId())) {
            throw new ResourceNotFoundException("ProductType", product.getProductTypeId());
        }
    }

    /** Fills the transient category and product type names the removed SQL JOIN used to provide. */
    private List<Product> enrich(List<Product> products) {
        if (products.isEmpty()) {
            return products;
        }
        Map<Integer, String> categories = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
        Map<Integer, String> types = productTypeRepository.findAll().stream()
                .collect(Collectors.toMap(ProductType::getId, ProductType::getName, (a, b) -> a));
        products.forEach(product -> {
            product.setCategoryName(categories.get(product.getCategoryId()));
            product.setProductTypeName(types.get(product.getProductTypeId()));
        });
        return products;
    }

    private String normalize(String nameFilter) {
        return nameFilter == null ? "" : nameFilter.trim();
    }

    private Integer normalizeCategory(Integer categoryId) {
        return (categoryId != null && categoryId > 0) ? categoryId : null;
    }
}
