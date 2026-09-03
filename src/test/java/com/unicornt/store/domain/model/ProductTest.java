package com.unicornt.store.domain.model;

import com.unicornt.store.domain.valueobject.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@DisplayName("Product domain model")
class ProductTest {

    private static Product valid() {
        return new Product(1L, "Classic Unicorn T-shirt", "Cotton", "classic-unicorn-tshirt",
                Money.ofClp(14990), 2L, "Unicorns", 3L, "T-shirt", 25, true);
    }

    @Nested
    @DisplayName("invariants")
    class Invariants {

        @Test
        @DisplayName("a fully specified product is accepted and trims its name")
        void acceptsValid() {
            Product product = new Product(1L, "  Hoodie  ", null, null, Money.ofClp(10), 1L, null, 1L, null, 0, false);

            assertThat(product.name()).isEqualTo("Hoodie");
            assertThat(product.description()).isEmpty();
            assertThat(product.imageBase()).isEmpty();
            assertThat(product.categoryName()).isEmpty();
            assertThat(product.productTypeName()).isEmpty();
            assertThat(product.isActive()).isFalse();
        }

        @Test
        @DisplayName("rejects a blank name")
        void rejectsBlankName() {
            Money price = Money.ofClp(10);
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> new Product(1L, "   ", null, null, price, 1L, null, 1L, null, 0, true))
                    .withMessageContaining("name is required");
        }

        @Test
        @DisplayName("rejects a null name")
        void rejectsNullName() {
            Money price = Money.ofClp(10);
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> new Product(1L, null, null, null, price, 1L, null, 1L, null, 0, true))
                    .withMessageContaining("name is required");
        }

        @Test
        @DisplayName("rejects a name longer than 200 characters")
        void rejectsLongName() {
            String tooLong = "x".repeat(201);
            Money price = Money.ofClp(10);
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> new Product(1L, tooLong, null, null, price, 1L, null, 1L, null, 0, true))
                    .withMessageContaining("200 characters");
        }

        @Test
        @DisplayName("accepts a name of exactly 200 characters")
        void acceptsBoundaryName() {
            String exact = "x".repeat(200);
            assertThat(new Product(1L, exact, null, null, Money.ofClp(10), 1L, null, 1L, null, 0, true).name())
                    .hasSize(200);
        }

        @Test
        @DisplayName("rejects a null price")
        void rejectsNullPrice() {
            assertThatExceptionOfType(NullPointerException.class)
                    .isThrownBy(() -> new Product(1L, "Hoodie", null, null, null, 1L, null, 1L, null, 0, true))
                    .withMessageContaining("price is required");
        }

        @Test
        @DisplayName("rejects a non-positive price")
        void rejectsZeroPrice() {
            Money zero = Money.ofClp(0);
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> new Product(1L, "Hoodie", null, null, zero, 1L, null, 1L, null, 0, true))
                    .withMessageContaining("price must be greater than 0");
        }

        @Test
        @DisplayName("rejects negative stock")
        void rejectsNegativeStock() {
            Money price = Money.ofClp(10);
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> new Product(1L, "Hoodie", null, null, price, 1L, null, 1L, null, -1, true))
                    .withMessageContaining("stock must not be negative");
        }

        @Test
        @DisplayName("rejects a non-positive category id")
        void rejectsCategoryId() {
            Money price = Money.ofClp(10);
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> new Product(1L, "Hoodie", null, null, price, 0L, null, 1L, null, 0, true))
                    .withMessageContaining("category must be referenced");
        }

        @Test
        @DisplayName("rejects a non-positive product type id")
        void rejectsProductTypeId() {
            Money price = Money.ofClp(10);
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> new Product(1L, "Hoodie", null, null, price, 1L, null, 0L, null, 0, true))
                    .withMessageContaining("product type must be referenced");
        }
    }

    @Test
    @DisplayName("create() builds an id-less product with no labels")
    void createFactory() {
        Product product = Product.create("Mug", "Ceramic", "rainbow-mug", Money.ofClp(7990), 2L, 3L, 10, true);

        assertThat(product.id()).isZero();
        assertThat(product.categoryName()).isEmpty();
        assertThat(product.productTypeName()).isEmpty();
        assertThat(product.stock()).isEqualTo(10);
    }

    @Test
    @DisplayName("withStock() returns a copy with the new stock and everything else unchanged")
    void withStockCopies() {
        Product original = valid();

        Product updated = original.withStock(3);

        assertThat(updated.stock()).isEqualTo(3);
        assertThat(updated.id()).isEqualTo(original.id());
        assertThat(updated.name()).isEqualTo(original.name());
        assertThat(updated.price()).isEqualTo(original.price());
        assertThat(updated.categoryName()).isEqualTo(original.categoryName());
    }

    @Test
    @DisplayName("hasStockFor() compares the requested units against the stock level")
    void hasStockFor() {
        Product product = valid().withStock(5);

        assertThat(product.hasStockFor(5)).isTrue();
        assertThat(product.hasStockFor(6)).isFalse();
    }

    @Test
    @DisplayName("exposes every field")
    void accessors() {
        Product product = valid();

        assertThat(product.id()).isEqualTo(1L);
        assertThat(product.name()).isEqualTo("Classic Unicorn T-shirt");
        assertThat(product.description()).isEqualTo("Cotton");
        assertThat(product.imageBase()).isEqualTo("classic-unicorn-tshirt");
        assertThat(product.price()).isEqualTo(Money.ofClp(14990));
        assertThat(product.categoryId()).isEqualTo(2L);
        assertThat(product.categoryName()).isEqualTo("Unicorns");
        assertThat(product.productTypeId()).isEqualTo(3L);
        assertThat(product.productTypeName()).isEqualTo("T-shirt");
        assertThat(product.stock()).isEqualTo(25);
        assertThat(product.isActive()).isTrue();
    }

    @Test
    @DisplayName("equality is based on the identifying and stateful fields")
    void equality() {
        assertThat(valid())
                .isEqualTo(valid())
                .hasSameHashCodeAs(valid())
                .isNotEqualTo(null)
                .isNotEqualTo("product");
    }

    @Test
    @DisplayName("equality distinguishes every field it compares")
    void equalityPerField() {
        Product base = valid();

        assertThat(base).isNotEqualTo(new Product(2L, base.name(), base.description(), base.imageBase(),
                base.price(), base.categoryId(), base.categoryName(), base.productTypeId(),
                base.productTypeName(), base.stock(), base.isActive()));
        assertThat(base).isNotEqualTo(new Product(base.id(), "Other name", base.description(), base.imageBase(),
                base.price(), base.categoryId(), base.categoryName(), base.productTypeId(),
                base.productTypeName(), base.stock(), base.isActive()));
        assertThat(base).isNotEqualTo(new Product(base.id(), base.name(), base.description(), base.imageBase(),
                com.unicornt.store.domain.valueobject.Money.ofClp(1), base.categoryId(), base.categoryName(),
                base.productTypeId(), base.productTypeName(), base.stock(), base.isActive()));
        assertThat(base).isNotEqualTo(new Product(base.id(), base.name(), base.description(), base.imageBase(),
                base.price(), 99L, base.categoryName(), base.productTypeId(),
                base.productTypeName(), base.stock(), base.isActive()));
        assertThat(base).isNotEqualTo(new Product(base.id(), base.name(), base.description(), base.imageBase(),
                base.price(), base.categoryId(), base.categoryName(), 99L,
                base.productTypeName(), base.stock(), base.isActive()));
        assertThat(base).isNotEqualTo(base.withStock(1));
        assertThat(base).isNotEqualTo(new Product(base.id(), base.name(), base.description(), base.imageBase(),
                base.price(), base.categoryId(), base.categoryName(), base.productTypeId(),
                base.productTypeName(), base.stock(), false));
    }

    @Test
    @DisplayName("toString names the product")
    void readableToString() {
        assertThat(valid().toString()).contains("Classic Unicorn T-shirt").contains("stock=25");
    }
}
