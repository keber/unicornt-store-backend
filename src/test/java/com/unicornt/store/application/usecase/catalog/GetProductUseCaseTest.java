package com.unicornt.store.application.usecase.catalog;

import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.domain.model.Product;
import com.unicornt.store.domain.repository.ProductRepository;
import com.unicornt.store.domain.valueobject.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetProductUseCase")
class GetProductUseCaseTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private GetProductUseCase useCase;

    @Test
    @DisplayName("returns the product when it exists")
    void returnsProduct() {
        Product product = new Product(7L, "Mug", "d", "mug", Money.ofClp(100), 1L, "C", 1L, "T", 5, true);
        when(productRepository.findById(7L)).thenReturn(Optional.of(product));

        assertThat(useCase.execute(7L)).isSameAs(product);
    }

    @Test
    @DisplayName("raises a not-found when the product is missing")
    void raisesNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> useCase.execute(99L))
                .withMessageContaining("Product not found: 99");
    }
}
