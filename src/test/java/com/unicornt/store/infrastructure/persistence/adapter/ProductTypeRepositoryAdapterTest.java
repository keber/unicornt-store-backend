package com.unicornt.store.infrastructure.persistence.adapter;

import com.unicornt.store.domain.model.ProductType;
import com.unicornt.store.infrastructure.persistence.entity.ProductTypeJpaEntity;
import com.unicornt.store.infrastructure.persistence.repository.SpringDataProductTypeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductTypeRepositoryAdapter")
class ProductTypeRepositoryAdapterTest {

    @Mock
    private SpringDataProductTypeRepository productTypes;
    @InjectMocks
    private ProductTypeRepositoryAdapter adapter;

    private static ProductTypeJpaEntity row(int id, String name) {
        ProductTypeJpaEntity entity = new ProductTypeJpaEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setSlug(name.toLowerCase());
        return entity;
    }

    @Test
    @DisplayName("findAll returns the id-ordered rows as domain models")
    void findAll() {
        when(productTypes.findAllByOrderByIdAsc()).thenReturn(List.of(row(1, "Tshirt"), row(2, "Mug")));

        assertThat(adapter.findAll()).extracting(ProductType::name).containsExactly("Tshirt", "Mug");
    }

    @Test
    @DisplayName("findById maps a present row and returns empty when absent")
    void findById() {
        when(productTypes.findById(1)).thenReturn(Optional.of(row(1, "Tshirt")));
        when(productTypes.findById(9)).thenReturn(Optional.empty());

        assertThat(adapter.findById(1L)).hasValueSatisfying(t -> assertThat(t.slug()).isEqualTo("tshirt"));
        assertThat(adapter.findById(9L)).isEmpty();
    }

    @Test
    @DisplayName("existsById delegates to the Spring Data repository")
    void existsById() {
        when(productTypes.existsById(1)).thenReturn(true);

        assertThat(adapter.existsById(1L)).isTrue();
    }
}
