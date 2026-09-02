package com.unicornt.store.application.usecase.catalog;

import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.domain.model.Product;
import com.unicornt.store.domain.repository.CategoryRepository;
import com.unicornt.store.domain.repository.ProductRepository;
import com.unicornt.store.domain.repository.ProductTypeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateProductUseCase")
class UpdateProductUseCaseTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductTypeRepository productTypeRepository;
    @InjectMocks
    private UpdateProductUseCase useCase;

    private static ProductCommand command() {
        return new ProductCommand("Renamed hoodie", "New copy", "hoodie", 19990, 3L, 1L, 12, false);
    }

    @Test
    @DisplayName("replaces the product, preserving its id, when everything checks out")
    void replaces() {
        when(productRepository.existsById(5L)).thenReturn(true);
        when(categoryRepository.existsById(3L)).thenReturn(true);
        when(productTypeRepository.existsById(1L)).thenReturn(true);
        when(productRepository.save(any(Product.class))).thenAnswer(call -> call.getArgument(0));

        useCase.execute(5L, command());

        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(saved.capture());
        assertThat(saved.getValue().id()).isEqualTo(5L);
        assertThat(saved.getValue().name()).isEqualTo("Renamed hoodie");
        assertThat(saved.getValue().stock()).isEqualTo(12);
        assertThat(saved.getValue().isActive()).isFalse();
    }

    @Test
    @DisplayName("fails with not-found when the product does not exist")
    void failsWhenMissing() {
        when(productRepository.existsById(5L)).thenReturn(false);

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> useCase.execute(5L, command()))
                .withMessageContaining("Product not found: 5");
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("fails with not-found when the new category does not exist")
    void failsOnMissingCategory() {
        when(productRepository.existsById(5L)).thenReturn(true);
        when(categoryRepository.existsById(3L)).thenReturn(false);

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> useCase.execute(5L, command()))
                .withMessageContaining("Category not found: 3");
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("fails with not-found when the new product type does not exist")
    void failsOnMissingType() {
        when(productRepository.existsById(5L)).thenReturn(true);
        when(categoryRepository.existsById(3L)).thenReturn(true);
        when(productTypeRepository.existsById(1L)).thenReturn(false);

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> useCase.execute(5L, command()))
                .withMessageContaining("Product type not found: 1");
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejects an invalid replacement before checking references")
    void rejectsInvalid() {
        when(productRepository.existsById(5L)).thenReturn(true);
        ProductCommand invalid = new ProductCommand("Hoodie", "d", "i", -1, 3L, 1L, 1, true);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> useCase.execute(5L, invalid));
        verify(categoryRepository, never()).existsById(any(Long.class));
    }
}
