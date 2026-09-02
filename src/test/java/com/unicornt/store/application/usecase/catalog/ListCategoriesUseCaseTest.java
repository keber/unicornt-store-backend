package com.unicornt.store.application.usecase.catalog;

import com.unicornt.store.domain.model.Category;
import com.unicornt.store.domain.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListCategoriesUseCase")
class ListCategoriesUseCaseTest {

    @Mock
    private CategoryRepository categoryRepository;
    @InjectMocks
    private ListCategoriesUseCase useCase;

    @Test
    @DisplayName("returns every category from the repository")
    void returnsAll() {
        List<Category> categories = List.of(
                new Category(1L, "Rainbows", "rainbows"),
                new Category(2L, "Unicorns", "unicorns"));
        when(categoryRepository.findAll()).thenReturn(categories);

        assertThat(useCase.execute()).isEqualTo(categories);
    }
}
