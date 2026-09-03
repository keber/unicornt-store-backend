package com.unicornt.store.infrastructure.persistence.adapter;

import com.unicornt.store.domain.model.Product;
import com.unicornt.store.domain.repository.PageResult;
import com.unicornt.store.domain.valueobject.Money;
import com.unicornt.store.infrastructure.persistence.entity.CategoryJpaEntity;
import com.unicornt.store.infrastructure.persistence.entity.ProductJpaEntity;
import com.unicornt.store.infrastructure.persistence.entity.ProductTypeJpaEntity;
import com.unicornt.store.infrastructure.persistence.repository.SpringDataCategoryRepository;
import com.unicornt.store.infrastructure.persistence.repository.SpringDataProductRepository;
import com.unicornt.store.infrastructure.persistence.repository.SpringDataProductTypeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductRepositoryAdapter")
class ProductRepositoryAdapterTest {

    @Mock
    private SpringDataProductRepository products;
    @Mock
    private SpringDataCategoryRepository categories;
    @Mock
    private SpringDataProductTypeRepository productTypes;
    @InjectMocks
    private ProductRepositoryAdapter adapter;

    private static ProductJpaEntity row(int id, int categoryId, int typeId) {
        ProductJpaEntity entity = new ProductJpaEntity();
        entity.setId(id);
        entity.setName("Product " + id);
        entity.setPrice(1000);
        entity.setCategoryId(categoryId);
        entity.setProductTypeId(typeId);
        entity.setStock(5);
        entity.setActive(true);
        return entity;
    }

    private static CategoryJpaEntity category(int id, String name) {
        CategoryJpaEntity entity = new CategoryJpaEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setSlug(name.toLowerCase());
        return entity;
    }

    private static ProductTypeJpaEntity type(int id, String name) {
        ProductTypeJpaEntity entity = new ProductTypeJpaEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setSlug(name.toLowerCase());
        return entity;
    }

    @Test
    @DisplayName("search maps the Spring page to a PageResult and fills the name labels")
    void searchMapsAndEnriches() {
        when(products.search(eq("unicorns"), eq("p"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(row(1, 3, 1)), PageRequest.of(0, 20), 1));
        when(categories.findAll()).thenReturn(List.of(category(3, "Unicorns")));
        when(productTypes.findAll()).thenReturn(List.of(type(1, "Tshirt")));

        PageResult<Product> page = adapter.search("unicorns", "p", 0, 20);

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(20);
        assertThat(page.content()).singleElement().satisfies(product -> {
            assertThat(product.categoryName()).isEqualTo("Unicorns");
            assertThat(product.productTypeName()).isEqualTo("Tshirt");
        });
    }

    @Test
    @DisplayName("search passes null filters through as empty strings")
    void searchNormalisesNullFilters() {
        when(products.search(eq(""), eq(""), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        adapter.search(null, null, 0, 20);

        verify(products).search(eq(""), eq(""), any(Pageable.class));
    }

    @Test
    @DisplayName("findById enriches and maps a single row")
    void findByIdMaps() {
        when(products.findById(1)).thenReturn(Optional.of(row(1, 3, 1)));
        when(categories.findAll()).thenReturn(List.of(category(3, "Unicorns")));
        when(productTypes.findAll()).thenReturn(List.of(type(1, "Tshirt")));

        assertThat(adapter.findById(1L)).hasValueSatisfying(product ->
                assertThat(product.categoryName()).isEqualTo("Unicorns"));
    }

    @Test
    @DisplayName("findById is empty when the row is missing")
    void findByIdEmpty() {
        when(products.findById(9)).thenReturn(Optional.empty());

        assertThat(adapter.findById(9L)).isEmpty();
    }

    @Test
    @DisplayName("save persists the mapped entity and returns the mapped result")
    void savePersists() {
        when(products.save(any(ProductJpaEntity.class))).thenReturn(row(5, 3, 1));
        when(categories.findAll()).thenReturn(List.of(category(3, "Unicorns")));
        when(productTypes.findAll()).thenReturn(List.of(type(1, "Tshirt")));

        Product saved = adapter.save(new Product(5L, "Product 5", "d", "i", Money.ofClp(1000),
                3L, null, 1L, null, 5, true));

        ArgumentCaptor<ProductJpaEntity> captor = ArgumentCaptor.forClass(ProductJpaEntity.class);
        verify(products).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Product 5");
        assertThat(saved.id()).isEqualTo(5L);
    }

    @Test
    @DisplayName("existsById and deleteById delegate to the Spring Data repository")
    void delegates() {
        when(products.existsById(3)).thenReturn(true);

        assertThat(adapter.existsById(3L)).isTrue();

        adapter.deleteById(3L);
        verify(products).deleteById(3);
    }

    @Test
    @DisplayName("an id past the int range is a miss, never a truncated lookup")
    void idOutsideIntRangeIsAMiss() {
        assertThat(adapter.findById(Integer.MAX_VALUE + 1L)).isEmpty();
        assertThat(adapter.existsById(Integer.MIN_VALUE - 1L)).isFalse();
        adapter.deleteById(Integer.MAX_VALUE + 1L);

        verifyNoInteractions(products);
    }

    @Test
    @DisplayName("an empty page yields an empty PageResult without a name lookup")
    void emptyPage() {
        when(products.search(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        assertThat(adapter.search("x", "y", 0, 20).content()).isEmpty();
    }
}
