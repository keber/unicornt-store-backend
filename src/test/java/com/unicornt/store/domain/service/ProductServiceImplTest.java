package com.unicornt.store.domain.service;

import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.infrastructure.persistence.entity.CategoryEntity;
import com.unicornt.store.infrastructure.persistence.entity.ProductEntity;
import com.unicornt.store.infrastructure.persistence.entity.ProductTypeEntity;
import com.unicornt.store.infrastructure.persistence.repository.CategoryRepository;
import com.unicornt.store.infrastructure.persistence.repository.ProductRepository;
import com.unicornt.store.infrastructure.persistence.repository.ProductTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Unit test of the product business rules (everything except {@code search}), no Spring context. */
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    private static final Sort BY_ID = Sort.by(Sort.Direction.ASC, "id");

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductTypeRepository productTypeRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    // ----------------------------------------------------------------
    // Object mothers
    // ----------------------------------------------------------------

    private static ProductEntity aProduct() {
        ProductEntity product = new ProductEntity();
        product.setId(7);
        product.setName("Unicorn Hoodie");
        product.setDescription("Cozy");
        product.setImageBase("hoodie");
        product.setPrice(4999);
        product.setCategoryId(3);
        product.setProductTypeId(1);
        product.setActive(true);
        return product;
    }

    private static CategoryEntity aCategory() {
        CategoryEntity category = new CategoryEntity();
        category.setId(3);
        category.setName("Hoodies");
        category.setSlug("hoodies");
        return category;
    }

    private static ProductTypeEntity aProductType() {
        ProductTypeEntity type = new ProductTypeEntity();
        type.setId(1);
        type.setName("Apparel");
        type.setSlug("apparel");
        return type;
    }

    /** Makes {@code validate} pass and {@code enrich} return names. */
    private void stubValidationAndEnrichmentToPass() {
        when(categoryRepository.existsById(3)).thenReturn(true);
        when(productTypeRepository.existsById(1)).thenReturn(true);
        when(categoryRepository.findAll()).thenReturn(List.of(aCategory()));
        when(productTypeRepository.findAll()).thenReturn(List.of(aProductType()));
    }

    @Nested
    @DisplayName("findAll(name, categoryId)")
    class FindAllUnpaged {

        @Test
        @DisplayName("Without a category it queries by name only, trimming the filter and sorting by id")
        void withoutCategory() {
            when(productRepository.findByNameContainingIgnoreCase(eq("hood"), any(Sort.class)))
                    .thenReturn(List.of());

            productService.findAll("  hood  ", null);

            ArgumentCaptor<Sort> sort = ArgumentCaptor.forClass(Sort.class);
            verify(productRepository).findByNameContainingIgnoreCase(eq("hood"), sort.capture());
            assertThat(sort.getValue()).isEqualTo(BY_ID);
            verify(productRepository, never())
                    .findByNameContainingIgnoreCaseAndCategoryId(any(), anyInt(), any(Sort.class));
        }

        @Test
        @DisplayName("A null filter reaches the repository as an empty string")
        void nullNameBecomesEmptyString() {
            when(productRepository.findByNameContainingIgnoreCase(eq(""), any(Sort.class)))
                    .thenReturn(List.of());

            productService.findAll(null, null);

            verify(productRepository).findByNameContainingIgnoreCase(eq(""), any(Sort.class));
        }

        @Test
        @DisplayName("A positive category id switches to the by-category query, sorted by id")
        void withCategory() {
            when(productRepository.findByNameContainingIgnoreCaseAndCategoryId(eq("hood"), eq(3), any(Sort.class)))
                    .thenReturn(List.of());

            productService.findAll("hood", 3);

            ArgumentCaptor<Sort> sort = ArgumentCaptor.forClass(Sort.class);
            verify(productRepository)
                    .findByNameContainingIgnoreCaseAndCategoryId(eq("hood"), eq(3), sort.capture());
            assertThat(sort.getValue()).isEqualTo(BY_ID);
        }

        @Test
        @DisplayName("A non-positive category id is treated as no category")
        void nonPositiveCategoryIsIgnored() {
            when(productRepository.findByNameContainingIgnoreCase(any(), any(Sort.class)))
                    .thenReturn(List.of());

            productService.findAll("hood", 0);

            verify(productRepository).findByNameContainingIgnoreCase(eq("hood"), any(Sort.class));
        }

        @Test
        @DisplayName("Returned products are enriched with category and product type names")
        void resultsAreEnriched() {
            ProductEntity product = aProduct();
            when(productRepository.findByNameContainingIgnoreCase(any(), any(Sort.class)))
                    .thenReturn(List.of(product));
            when(categoryRepository.findAll()).thenReturn(List.of(aCategory()));
            when(productTypeRepository.findAll()).thenReturn(List.of(aProductType()));

            ProductEntity found = productService.findAll("hood", null).get(0);

            assertThat(found.getCategoryName()).isEqualTo("Hoodies");
            assertThat(found.getProductTypeName()).isEqualTo("Apparel");
        }
    }

    @Nested
    @DisplayName("findAll(name, categoryId, limit, offset)")
    class FindAllPaged {

        @Test
        @DisplayName("A limit of zero or less raises IllegalArgumentException")
        void nonPositiveLimitThrows() {
            assertThatThrownBy(() -> productService.findAll("x", null, 0, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("limit must be greater than 0");
            verifyNoInteractions(productRepository);
        }

        @Test
        @DisplayName("A negative offset raises IllegalArgumentException")
        void negativeOffsetThrows() {
            assertThatThrownBy(() -> productService.findAll("x", null, 10, -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("offset must not be negative");
            verifyNoInteractions(productRepository);
        }

        @Test
        @DisplayName("The page number is offset divided by limit, sized by limit and sorted by id")
        void computesPageNumber() {
            when(productRepository.findByNameContainingIgnoreCase(any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            productService.findAll("hood", null, 10, 25);

            ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
            verify(productRepository).findByNameContainingIgnoreCase(eq("hood"), page.capture());
            assertThat(page.getValue().getPageNumber()).isEqualTo(2);
            assertThat(page.getValue().getPageSize()).isEqualTo(10);
            assertThat(page.getValue().getSort()).isEqualTo(BY_ID);
        }

        @Test
        @DisplayName("A positive category id switches to the paginated by-category query")
        void withCategoryUsesCategoryQuery() {
            when(productRepository.findByNameContainingIgnoreCaseAndCategoryId(any(), eq(3), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            productService.findAll("hood", 3, 5, 0);

            verify(productRepository)
                    .findByNameContainingIgnoreCaseAndCategoryId(eq("hood"), eq(3), any(Pageable.class));
            verify(productRepository, never())
                    .findByNameContainingIgnoreCase(any(), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("countAll")
    class CountAll {

        @Test
        @DisplayName("Without a category it counts by name only, trimming the filter")
        void withoutCategory() {
            when(productRepository.countByNameContainingIgnoreCase("hood")).thenReturn(4L);

            long count = productService.countAll("  hood  ", null);

            assertThat(count).isEqualTo(4L);
            verify(productRepository, never())
                    .countByNameContainingIgnoreCaseAndCategoryId(any(), anyInt());
        }

        @Test
        @DisplayName("A positive category id counts within that category")
        void withCategory() {
            when(productRepository.countByNameContainingIgnoreCaseAndCategoryId("hood", 3)).thenReturn(2L);

            long count = productService.countAll("hood", 3);

            assertThat(count).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("A known id returns the enriched product")
        void known() {
            ProductEntity product = aProduct();
            when(productRepository.findById(7)).thenReturn(Optional.of(product));
            when(categoryRepository.findAll()).thenReturn(List.of(aCategory()));
            when(productTypeRepository.findAll()).thenReturn(List.of(aProductType()));

            ProductEntity found = productService.findById(7);

            assertThat(found.getCategoryName()).isEqualTo("Hoodies");
            assertThat(found.getProductTypeName()).isEqualTo("Apparel");
        }

        @Test
        @DisplayName("An unknown id raises ResourceNotFoundException")
        void unknown() {
            when(productRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.findById(99))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Product not found: 99");
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("A valid product is saved with a reset id and returned enriched")
        void happyPath() {
            ProductEntity product = aProduct();
            product.setId(555);
            stubValidationAndEnrichmentToPass();
            when(productRepository.save(any(ProductEntity.class))).thenAnswer(call -> call.getArgument(0));

            ProductEntity created = productService.create(product);

            ArgumentCaptor<ProductEntity> saved = ArgumentCaptor.forClass(ProductEntity.class);
            verify(productRepository).save(saved.capture());
            assertThat(saved.getValue().getId()).isZero();
            assertThat(created.getCategoryName()).isEqualTo("Hoodies");
            assertThat(created.getProductTypeName()).isEqualTo("Apparel");
        }

        @Test
        @DisplayName("A null name is rejected before any save")
        void nullName() {
            ProductEntity product = aProduct();
            product.setName(null);

            assertThatThrownBy(() -> productService.create(product))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Product name is required");
            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("A blank name is rejected")
        void blankName() {
            ProductEntity product = aProduct();
            product.setName("   ");

            assertThatThrownBy(() -> productService.create(product))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Product name is required");
        }

        @Test
        @DisplayName("A name longer than 200 characters is rejected")
        void nameTooLong() {
            ProductEntity product = aProduct();
            product.setName("x".repeat(201));

            assertThatThrownBy(() -> productService.create(product))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Product name must not exceed 200 characters");
        }

        @Test
        @DisplayName("A price of zero or less is rejected")
        void nonPositivePrice() {
            ProductEntity product = aProduct();
            product.setPrice(0);

            assertThatThrownBy(() -> productService.create(product))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Price must be an integer greater than 0");
        }

        @Test
        @DisplayName("A category id of zero or less is rejected")
        void nonPositiveCategoryId() {
            ProductEntity product = aProduct();
            product.setCategoryId(0);

            assertThatThrownBy(() -> productService.create(product))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("A category must be selected");
        }

        @Test
        @DisplayName("An unknown category raises ResourceNotFoundException")
        void unknownCategory() {
            ProductEntity product = aProduct();
            when(categoryRepository.existsById(3)).thenReturn(false);

            assertThatThrownBy(() -> productService.create(product))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Category not found: 3");
        }

        @Test
        @DisplayName("A product type id of zero or less is rejected")
        void nonPositiveProductTypeId() {
            ProductEntity product = aProduct();
            product.setProductTypeId(0);
            when(categoryRepository.existsById(3)).thenReturn(true);

            assertThatThrownBy(() -> productService.create(product))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("A product type must be selected");
        }

        @Test
        @DisplayName("An unknown product type raises ResourceNotFoundException")
        void unknownProductType() {
            ProductEntity product = aProduct();
            when(categoryRepository.existsById(3)).thenReturn(true);
            when(productTypeRepository.existsById(1)).thenReturn(false);

            assertThatThrownBy(() -> productService.create(product))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Product type not found: 1");
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("A valid update copies every field onto the existing row and returns it enriched")
        void happyPath() {
            ProductEntity existing = aProduct();
            existing.setName("Old");
            existing.setPrice(1);
            ProductEntity incoming = aProduct();
            incoming.setName("New Hoodie");
            incoming.setDescription("Fresh");
            incoming.setImageBase("new");
            incoming.setPrice(8000);
            incoming.setActive(false);

            when(productRepository.findById(7)).thenReturn(Optional.of(existing));
            stubValidationAndEnrichmentToPass();
            when(productRepository.save(any(ProductEntity.class))).thenAnswer(call -> call.getArgument(0));

            ProductEntity updated = productService.update(7, incoming);

            assertThat(updated.getName()).isEqualTo("New Hoodie");
            assertThat(updated.getDescription()).isEqualTo("Fresh");
            assertThat(updated.getImageBase()).isEqualTo("new");
            assertThat(updated.getPrice()).isEqualTo(8000);
            assertThat(updated.isActive()).isFalse();
            assertThat(updated.getCategoryName()).isEqualTo("Hoodies");
            verify(productRepository).save(existing);
        }

        @Test
        @DisplayName("An unknown id raises ResourceNotFoundException before validation")
        void unknownId() {
            when(productRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.update(99, aProduct()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Product not found: 99");
            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("The incoming product still runs the full validation")
        void validatesIncoming() {
            when(productRepository.findById(7)).thenReturn(Optional.of(aProduct()));
            ProductEntity incoming = aProduct();
            incoming.setName(" ");

            assertThatThrownBy(() -> productService.update(7, incoming))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Product name is required");
            verify(productRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("An existing id is deleted by id")
        void existing() {
            when(productRepository.existsById(7)).thenReturn(true);

            productService.delete(7);

            verify(productRepository).deleteById(7);
        }

        @Test
        @DisplayName("A missing id raises ResourceNotFoundException and deletes nothing")
        void missing() {
            when(productRepository.existsById(99)).thenReturn(false);

            assertThatThrownBy(() -> productService.delete(99))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Product not found: 99");
            verify(productRepository, never()).deleteById(anyInt());
        }
    }

    @Nested
    @DisplayName("enrich")
    class Enrich {

        @Test
        @DisplayName("Names are resolved per product from the category and product type repositories")
        void fillsNamesFromRepos() {
            ProductEntity hoodie = aProduct();
            hoodie.setCategoryId(3);
            hoodie.setProductTypeId(1);
            ProductEntity mug = aProduct();
            mug.setId(8);
            mug.setCategoryId(5);
            mug.setProductTypeId(2);

            CategoryEntity mugs = new CategoryEntity();
            mugs.setId(5);
            mugs.setName("Mugs");
            ProductTypeEntity drinkware = new ProductTypeEntity();
            drinkware.setId(2);
            drinkware.setName("Drinkware");

            when(productRepository.findByNameContainingIgnoreCase(any(), any(Sort.class)))
                    .thenReturn(List.of(hoodie, mug));
            when(categoryRepository.findAll()).thenReturn(List.of(aCategory(), mugs));
            when(productTypeRepository.findAll()).thenReturn(List.of(aProductType(), drinkware));

            List<ProductEntity> result = productService.findAll(null, null);

            assertThat(result).extracting(ProductEntity::getCategoryName)
                    .containsExactly("Hoodies", "Mugs");
            assertThat(result).extracting(ProductEntity::getProductTypeName)
                    .containsExactly("Apparel", "Drinkware");
        }

        @Test
        @DisplayName("An empty result short-circuits without hitting the lookup repositories")
        void emptyResultSkipsLookups() {
            when(productRepository.findByNameContainingIgnoreCase(any(), any(Sort.class)))
                    .thenReturn(List.of());

            List<ProductEntity> result = productService.findAll(null, null);

            assertThat(result).isEmpty();
            verify(categoryRepository, never()).findAll();
            verify(productTypeRepository, never()).findAll();
        }
    }

    @Nested
    @DisplayName("findAllProductTypes")
    class FindAllProductTypes {

        @Test
        @DisplayName("Delegates to the product type repository ordered by id")
        void delegates() {
            List<ProductTypeEntity> types = List.of(aProductType());
            when(productTypeRepository.findAllByOrderByIdAsc()).thenReturn(types);

            assertThat(productService.findAllProductTypes()).isSameAs(types);
        }
    }
}
