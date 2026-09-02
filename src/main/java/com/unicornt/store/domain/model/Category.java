package com.unicornt.store.domain.model;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

/**
 * A product category (Unicorns, Rainbows, Stars).
 *
 * <p>Plain Java. Invariants are enforced on construction: the name is required and
 * at most {@value #MAX_NAME_LENGTH} characters, and the slug is a non-empty,
 * lower-case, accent-free, hyphen-separated token. Slug <em>uniqueness</em> spans
 * the whole catalog, so it is checked by the use case against the repository, not
 * here.</p>
 */
public final class Category {

    public static final int MAX_NAME_LENGTH = 100;

    private final long id;
    private final String name;
    private final String slug;

    public Category(long id, String name, String slug) {
        String trimmedName = name == null ? "" : name.trim();
        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("category name is required");
        }
        if (trimmedName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "category name must not exceed " + MAX_NAME_LENGTH + " characters");
        }
        String normalizedSlug = slug == null || slug.isBlank() ? slugify(trimmedName) : slugify(slug);
        this.id = id;
        this.name = trimmedName;
        this.slug = normalizedSlug;
    }

    /** A brand-new category (no id yet); the slug is derived from the name when blank. */
    public static Category create(String name, String slug) {
        return new Category(0L, name, slug);
    }

    /**
     * Builds a URL-friendly identifier: lower case, accent free, hyphen separated.
     * NFD splits an accented letter into a base letter plus a combining mark; the
     * alphanumeric filter then drops the marks. Throws when nothing survives.
     */
    public static String slugify(String value) {
        String decomposed = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD);
        String withoutMarks = decomposed.replaceAll("\\p{M}+", "");
        String slug = withoutMarks.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (slug.isBlank()) {
            throw new IllegalArgumentException(
                    "category name must contain at least one alphanumeric character");
        }
        return slug;
    }

    public long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String slug() {
        return slug;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Category category
                && category.id == this.id
                && category.name.equals(this.name)
                && category.slug.equals(this.slug);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, slug);
    }

    @Override
    public String toString() {
        return "Category{id=" + id + ", name='" + name + "', slug='" + slug + "'}";
    }
}
