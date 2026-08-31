package com.unicornt.store.domain.service;

import com.unicornt.store.infrastructure.persistence.entity.CategoryEntity;
import com.unicornt.store.infrastructure.persistence.entity.ProductEntity;
import com.unicornt.store.infrastructure.persistence.entity.ProductTypeEntity;
import com.unicornt.store.infrastructure.persistence.repository.CategoryRepository;
import com.unicornt.store.infrastructure.persistence.repository.ProductRepository;
import com.unicornt.store.infrastructure.persistence.repository.ProductTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit test of the paginated catalog search added for the REST list endpoint. */
class ProductServiceImplSearchTest {

    private ProductRepository productRepository;
    private CategoryRepository categoryRepository;
    private ProductTypeRepository productTypeRepository;
    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        productTypeRepository = mock(ProductTypeRepository.class);
        productService = new ProductServiceImpl(productRepository, categoryRepository, productTypeRepository);
    }

    @Test
    @DisplayName("Null filters reach the repository as empty strings, which disables them")
    void nullFiltersBecomeEmptyStrings() {
        when(productRepository.search(anyString(), anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        productService.search(null, null, PageRequest.of(0, 20));

        verify(productRepository).search(eq(""), eq(""), any(Pageable.class));
    }

    @Test
    @DisplayName("An unsorted page request is completed with the default id ordering")
    void unsortedRequestGetsDefaultSort() {
        when(productRepository.search(anyString(), anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        productService.search("hoodies", "unicorn", PageRequest.of(1, 5));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).search(anyString(), anyString(), captor.capture());
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "id"));
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    @DisplayName("Returned products carry the category and product type names")
    void resultsAreEnriched() {
        ProductEntity product = new ProductEntity();
        product.setId(42);
        product.setCategoryId(3);
        product.setProductTypeId(1);
        when(productRepository.search(anyString(), anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));
        when(categoryRepository.findAll()).thenReturn(List.of(category(3, "Hoodies")));
        when(productTypeRepository.findAll()).thenReturn(List.of(productType(1, "Apparel")));

        ProductEntity found = productService.search("", "", PageRequest.of(0, 20)).getContent().get(0);

        assertThat(found.getCategoryName()).isEqualTo("Hoodies");
        assertThat(found.getProductTypeName()).isEqualTo("Apparel");
    }

    private CategoryEntity category(int id, String name) {
        CategoryEntity entity = new CategoryEntity();
        entity.setId(id);
        entity.setName(name);
        return entity;
    }

    private ProductTypeEntity productType(int id, String name) {
        ProductTypeEntity entity = new ProductTypeEntity();
        entity.setId(id);
        entity.setName(name);
        return entity;
    }
}
