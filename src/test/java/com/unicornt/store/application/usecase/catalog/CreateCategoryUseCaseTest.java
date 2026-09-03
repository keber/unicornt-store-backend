package com.unicornt.store.application.usecase.catalog;

import com.unicornt.store.domain.exception.DuplicateResourceException;
import com.unicornt.store.domain.model.Category;
import com.unicornt.store.domain.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateCategoryUseCase")
class CreateCategoryUseCaseTest {

    @Mock
    private CategoryRepository categoryRepository;
    @InjectMocks
    private CreateCategoryUseCase useCase;

    @Test
    @DisplayName("derives the slug, checks uniqueness and saves")
    void savesWithDerivedSlug() {
        when(categoryRepository.findBySlug("rainbow-mugs")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(call -> call.getArgument(0));

        Category created = useCase.execute("Rainbow Mugs", null);

        ArgumentCaptor<Category> saved = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(saved.capture());
        assertThat(saved.getValue().slug()).isEqualTo("rainbow-mugs");
        assertThat(created.name()).isEqualTo("Rainbow Mugs");
    }

    @Test
    @DisplayName("rejects a duplicate slug with a conflict and never saves")
    void rejectsDuplicate() {
        when(categoryRepository.findBySlug("hoodies"))
                .thenReturn(Optional.of(new Category(1L, "Hoodies", "hoodies")));

        assertThatExceptionOfType(DuplicateResourceException.class)
                .isThrownBy(() -> useCase.execute("Hoodies", "hoodies"))
                .withMessageContaining("slug");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("propagates the domain rejection of a nameless category")
    void rejectsBlankName() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> useCase.execute("   ", null));
        verify(categoryRepository, never()).save(any());
    }
}
