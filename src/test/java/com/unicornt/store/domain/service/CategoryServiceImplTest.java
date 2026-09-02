package com.unicornt.store.domain.service;

import com.unicornt.store.domain.exception.DuplicateResourceException;
import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.infrastructure.persistence.entity.CategoryEntity;
import com.unicornt.store.infrastructure.persistence.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit test of the category business rules, with no Spring context. */
class CategoryServiceImplTest {

    private CategoryRepository categoryRepository;
    private CategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        categoryRepository = mock(CategoryRepository.class);
        categoryService = new CategoryServiceImpl(categoryRepository);
    }

    @Test
    @DisplayName("An unknown id raises ResourceNotFoundException")
    void findByIdUnknownThrows() {
        when(categoryRepository.findById(anyInt())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findById(99))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found: 99");
    }

    @Test
    @DisplayName("The slug is derived from the name when it is not supplied")
    void createDerivesSlug() {
        when(categoryRepository.findBySlug(anyString())).thenReturn(Optional.empty());
        when(categoryRepository.save(any(CategoryEntity.class))).thenAnswer(call -> call.getArgument(0));

        CategoryEntity input = new CategoryEntity();
        input.setName("Winter Hoodies");

        assertThat(categoryService.create(input).getSlug()).isEqualTo("winter-hoodies");
    }

    @Test
    @DisplayName("A taken slug raises DuplicateResourceException")
    void createDuplicateSlugThrows() {
        when(categoryRepository.findBySlug("hoodies")).thenReturn(Optional.of(new CategoryEntity()));

        CategoryEntity input = new CategoryEntity();
        input.setName("Hoodies");

        assertThatThrownBy(() -> categoryService.create(input))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("A blank name raises IllegalArgumentException")
    void createBlankNameThrows() {
        CategoryEntity input = new CategoryEntity();
        input.setName("  ");

        assertThatThrownBy(() -> categoryService.create(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Category name is required");
    }

    @Test
    @DisplayName("findAll delegates to the repository's name-ordered query")
    void findAllDelegatesToOrderedQuery() {
        CategoryEntity a = new CategoryEntity();
        a.setName("Apparel");
        CategoryEntity b = new CategoryEntity();
        b.setName("Books");
        when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(a, b));

        List<CategoryEntity> result = categoryService.findAll();

        assertThat(result).containsExactly(a, b);
        verify(categoryRepository).findAllByOrderByNameAsc();
    }

    @Test
    @DisplayName("An explicit non-blank slug is slugified and not derived from the name")
    void createUsesExplicitSlug() {
        when(categoryRepository.findBySlug(anyString())).thenReturn(Optional.empty());
        when(categoryRepository.save(any(CategoryEntity.class))).thenAnswer(call -> call.getArgument(0));

        CategoryEntity input = new CategoryEntity();
        input.setName("Winter Hoodies");
        input.setSlug("Cozy Winter Wear!");

        assertThat(categoryService.create(input).getSlug()).isEqualTo("cozy-winter-wear");
    }

    @Test
    @DisplayName("A name longer than 100 characters raises IllegalArgumentException")
    void createNameTooLongThrows() {
        CategoryEntity input = new CategoryEntity();
        input.setName("x".repeat(101));

        assertThatThrownBy(() -> categoryService.create(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Category name must not exceed 100 characters");
    }

    @Test
    @DisplayName("A name that reduces to nothing after slugify raises IllegalArgumentException")
    void createNameWithoutAlphanumericThrows() {
        CategoryEntity input = new CategoryEntity();
        input.setName("!!!");

        assertThatThrownBy(() -> categoryService.create(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Category name must contain at least one alphanumeric character");
    }

    @Test
    @DisplayName("An explicit slug that reduces to nothing after slugify raises IllegalArgumentException")
    void createSlugWithoutAlphanumericThrows() {
        CategoryEntity input = new CategoryEntity();
        input.setName("Valid Name");
        input.setSlug("---");

        assertThatThrownBy(() -> categoryService.create(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Category name must contain at least one alphanumeric character");
    }
}
