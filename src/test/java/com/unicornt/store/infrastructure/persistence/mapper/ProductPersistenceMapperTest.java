package com.unicornt.store.infrastructure.persistence.mapper;

import com.unicornt.store.domain.model.Product;
import com.unicornt.store.domain.valueobject.Money;
import com.unicornt.store.infrastructure.persistence.entity.ProductJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProductPersistenceMapper")
class ProductPersistenceMapperTest {

    @Test
    @DisplayName("maps a row (with its transient labels) to the domain model")
    void toDomain() {
        ProductJpaEntity entity = new ProductJpaEntity();
        entity.setId(42);
        entity.setName("Unicorn hoodie");
        entity.setDescription("Cotton");
        entity.setImageBase("hoodie");
        entity.setPrice(25990);
        entity.setCategoryId(3);
        entity.setCategoryName("Unicorns");
        entity.setProductTypeId(1);
        entity.setProductTypeName("T-shirt");
        entity.setStock(40);
        entity.setActive(true);

        Product product = ProductPersistenceMapper.toDomain(entity);

        assertThat(product.id()).isEqualTo(42L);
        assertThat(product.name()).isEqualTo("Unicorn hoodie");
        assertThat(product.price()).isEqualTo(Money.ofClp(25990));
        assertThat(product.categoryName()).isEqualTo("Unicorns");
        assertThat(product.productTypeName()).isEqualTo("T-shirt");
        assertThat(product.stock()).isEqualTo(40);
        assertThat(product.isActive()).isTrue();
    }

    @Test
    @DisplayName("maps the domain model back to a new row without the transient labels")
    void toEntity() {
        Product product = new Product(7L, "Mug", "Ceramic", "mug", Money.ofClp(7990),
                2L, "Rainbows", 2L, "Mug", 10, false);

        ProductJpaEntity entity = ProductPersistenceMapper.toEntity(product);

        assertThat(entity.getId()).isEqualTo(7);
        assertThat(entity.getName()).isEqualTo("Mug");
        assertThat(entity.getPrice()).isEqualTo(7990);
        assertThat(entity.getCategoryId()).isEqualTo(2);
        assertThat(entity.getProductTypeId()).isEqualTo(2);
        assertThat(entity.getStock()).isEqualTo(10);
        assertThat(entity.isActive()).isFalse();
        assertThat(entity.getCategoryName()).isEmpty();
        assertThat(entity.getProductTypeName()).isEmpty();
    }

    @Test
    @DisplayName("applyColumns overwrites the mutable columns of an existing row")
    void applyColumns() {
        ProductJpaEntity existing = new ProductJpaEntity();
        existing.setId(9);
        existing.setName("Old");

        ProductPersistenceMapper.applyColumns(existing,
                new Product(9L, "New", "d", "img", Money.ofClp(100), 1L, null, 1L, null, 3, true));

        assertThat(existing.getName()).isEqualTo("New");
        assertThat(existing.getStock()).isEqualTo(3);
        assertThat(existing.getPrice()).isEqualTo(100);
    }
}
