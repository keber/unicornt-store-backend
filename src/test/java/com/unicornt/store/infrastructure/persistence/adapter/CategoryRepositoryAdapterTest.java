package com.unicornt.store.infrastructure.persistence.adapter;

import com.unicornt.store.domain.model.Category;
import com.unicornt.store.infrastructure.persistence.entity.CategoryJpaEntity;
import com.unicornt.store.infrastructure.persistence.repository.SpringDataCategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryRepositoryAdapter")
class CategoryRepositoryAdapterTest {

    @Mock
    private SpringDataCategoryRepository categories;
    @InjectMocks
    private CategoryRepositoryAdapter adapter;

    private static CategoryJpaEntity row(int id, String name) {
        CategoryJpaEntity entity = new CategoryJpaEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setSlug(name.toLowerCase());
        return entity;
    }

    @Test
    @DisplayName("findAll returns the name-ordered rows as domain models")
    void findAll() {
        when(categories.findAllByOrderByNameAsc())
                .thenReturn(List.of(row(1, "Rainbows"), row(2, "Unicorns")));

        assertThat(adapter.findAll()).extracting(Category::name)
                .containsExactly("Rainbows", "Unicorns");
    }

    @Test
    @DisplayName("findById maps a present row and returns empty when absent")
    void findById() {
        when(categories.findById(3)).thenReturn(Optional.of(row(3, "Stars")));
        when(categories.findById(9)).thenReturn(Optional.empty());

        assertThat(adapter.findById(3L)).hasValueSatisfying(c -> assertThat(c.slug()).isEqualTo("stars"));
        assertThat(adapter.findById(9L)).isEmpty();
    }

    @Test
    @DisplayName("existsById delegates to the Spring Data repository")
    void existsById() {
        when(categories.existsById(3)).thenReturn(true);

        assertThat(adapter.existsById(3L)).isTrue();
    }

    @Test
    @DisplayName("an id past the int range is a miss, never a truncated lookup")
    void idOutsideIntRangeIsAMiss() {
        long overflowing = Integer.MAX_VALUE + 1L;

        assertThat(adapter.findById(overflowing)).isEmpty();
        assertThat(adapter.existsById(overflowing)).isFalse();

        verifyNoInteractions(categories);
    }

    @Test
    @DisplayName("findBySlug maps the matched row")
    void findBySlug() {
        when(categories.findBySlug("stars")).thenReturn(Optional.of(row(3, "Stars")));

        assertThat(adapter.findBySlug("stars")).hasValueSatisfying(c -> assertThat(c.id()).isEqualTo(3L));
    }

    @Test
    @DisplayName("save persists the mapped entity")
    void save() {
        when(categories.save(any(CategoryJpaEntity.class))).thenReturn(row(7, "Stars"));

        Category saved = adapter.save(new Category(0L, "Stars", "stars"));

        ArgumentCaptor<CategoryJpaEntity> captor = ArgumentCaptor.forClass(CategoryJpaEntity.class);
        verify(categories).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Stars");
        assertThat(saved.id()).isEqualTo(7L);
    }
}
