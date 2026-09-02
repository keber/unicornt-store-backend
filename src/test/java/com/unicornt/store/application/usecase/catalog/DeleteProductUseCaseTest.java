package com.unicornt.store.application.usecase.catalog;

import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.domain.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteProductUseCase")
class DeleteProductUseCaseTest {

    @Mock
    private ProductRepository productRepository;
    @InjectMocks
    private DeleteProductUseCase useCase;

    @Test
    @DisplayName("deletes an existing product")
    void deletesExisting() {
        when(productRepository.existsById(4L)).thenReturn(true);

        useCase.execute(4L);

        verify(productRepository).deleteById(4L);
    }

    @Test
    @DisplayName("fails with not-found and never deletes when the product is missing")
    void failsWhenMissing() {
        when(productRepository.existsById(4L)).thenReturn(false);

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> useCase.execute(4L))
                .withMessageContaining("Product not found: 4");
        verify(productRepository, never()).deleteById(4L);
    }
}
