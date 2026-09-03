package com.unicornt.store.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@DisplayName("Category domain model")
class CategoryTest {

    @Test
    @DisplayName("keeps the given slug when one is supplied")
    void keepsExplicitSlug() {
        Category category = new Category(1L, "Special Offers", "promos");

        assertThat(category.slug()).isEqualTo("promos");
        assertThat(category.name()).isEqualTo("Special Offers");
        assertThat(category.id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("derives the slug from the name when the slug is blank")
    void derivesSlug() {
        assertThat(new Category(0L, "Rainbow Mugs", "  ").slug()).isEqualTo("rainbow-mugs");
        assertThat(new Category(0L, "Rainbow Mugs", null).slug()).isEqualTo("rainbow-mugs");
    }

    @Test
    @DisplayName("slugify lowercases, strips accents and collapses non-alphanumerics to single hyphens")
    void slugifyNormalises() {
        assertThat(Category.slugify("Ñandú  &  Café!!")).isEqualTo("nandu-cafe");
        assertThat(Category.slugify("--Edge--")).isEqualTo("edge");
    }

    @Test
    @DisplayName("slugify rejects a value with no alphanumeric character, including null")
    void slugifyRejectsEmpty() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Category.slugify("---"))
                .withMessageContaining("at least one alphanumeric");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Category.slugify(null))
                .withMessageContaining("at least one alphanumeric");
    }

    @Test
    @DisplayName("trims and requires the name")
    void requiresName() {
        assertThat(new Category(1L, "  Hoodies  ", "hoodies").name()).isEqualTo("Hoodies");

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Category(1L, "   ", "x"))
                .withMessageContaining("name is required");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Category(1L, null, "x"))
                .withMessageContaining("name is required");
    }

    @Test
    @DisplayName("rejects a name longer than 100 characters and accepts exactly 100")
    void nameLength() {
        assertThat(new Category(1L, "x".repeat(100), "s").name()).hasSize(100);

        String tooLong = "x".repeat(101);
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Category(1L, tooLong, "s"))
                .withMessageContaining("100 characters");
    }

    @Test
    @DisplayName("create() builds an id-less category")
    void createFactory() {
        assertThat(Category.create("Stars", null).id()).isZero();
    }

    @Test
    @DisplayName("equality is value based")
    void equality() {
        assertThat(new Category(1L, "Stars", "stars"))
                .isEqualTo(new Category(1L, "Stars", "stars"))
                .hasSameHashCodeAs(new Category(1L, "Stars", "stars"))
                .isNotEqualTo(new Category(2L, "Stars", "stars"))
                .isNotEqualTo(new Category(1L, "Star", "stars"))
                .isNotEqualTo(new Category(1L, "Stars", "star"))
                .isNotEqualTo(null)
                .isNotEqualTo("stars");
    }

    @Test
    @DisplayName("toString shows the slug")
    void readableToString() {
        assertThat(new Category(1L, "Stars", "stars").toString()).contains("slug='stars'");
    }
}
