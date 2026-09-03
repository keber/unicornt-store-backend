package com.unicornt.store.application.usecase.catalog;

import com.unicornt.store.domain.model.Product;
import com.unicornt.store.domain.repository.PageResult;
import com.unicornt.store.domain.repository.ProductRepository;
import com.unicornt.store.domain.valueobject.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListProductsUseCase")
class ListProductsUseCaseTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ListProductsUseCase useCase;

    private static Product product() {
        return new Product(1L, "Mug", "d", "mug", Money.ofClp(100), 1L, "C", 1L, "T", 5, true);
    }

    @Test
    @DisplayName("passes the trimmed filters and normalised paging to the repository")
    void delegatesWithNormalisedArguments() {
        when(productRepository.search("unicorns", "star", 1, 15))
                .thenReturn(new PageResult<>(List.of(product()), 1, 15, 1));

        PageResult<Product> result = useCase.execute("  unicorns ", " star ", 1, 15);

        assertThat(result.content()).hasSize(1);
        verify(productRepository).search("unicorns", "star", 1, 15);
    }

    @Test
    @DisplayName("turns blank filters into null so the repository disables them")
    void blankFiltersBecomeNull() {
        when(productRepository.search(eq(null), eq(null), eq(0), eq(20)))
                .thenReturn(new PageResult<>(List.of(), 0, 20, 0));

        useCase.execute("   ", null, 0, 20);

        verify(productRepository).search(null, null, 0, 20);
    }

    @Test
    @DisplayName("clamps a negative page to 0 and a huge size to the maximum")
    void clampsPaging() {
        when(productRepository.search(any(), any(), eq(0), eq(ListProductsUseCase.MAX_PAGE_SIZE)))
                .thenReturn(new PageResult<>(List.of(), 0, ListProductsUseCase.MAX_PAGE_SIZE, 0));

        useCase.execute(null, null, -3, 10_000);

        ArgumentCaptor<Integer> page = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> size = ArgumentCaptor.forClass(Integer.class);
        verify(productRepository).search(any(), any(), page.capture(), size.capture());
        assertThat(page.getValue()).isZero();
        assertThat(size.getValue()).isEqualTo(ListProductsUseCase.MAX_PAGE_SIZE);
    }

    @Test
    @DisplayName("falls back to the default size when the requested size is not positive")
    void defaultsSize() {
        when(productRepository.search(any(), any(), eq(0), eq(ListProductsUseCase.DEFAULT_PAGE_SIZE)))
                .thenReturn(new PageResult<>(List.of(), 0, ListProductsUseCase.DEFAULT_PAGE_SIZE, 0));

        useCase.execute(null, null, 0, 0);

        verify(productRepository).search(null, null, 0, ListProductsUseCase.DEFAULT_PAGE_SIZE);
    }
}
