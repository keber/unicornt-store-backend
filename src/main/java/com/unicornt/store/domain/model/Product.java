package com.unicornt.store.domain.model;

import com.unicornt.store.domain.valueobject.Money;

import java.util.Objects;

/**
 * A catalog product.
 *
 * <p>Plain Java. Invariants enforced on construction (PLAN.md &sect;2.4):
 * the name is required and at most {@value #MAX_NAME_LENGTH} characters, the price
 * is strictly positive, the stock is not negative, and both the category and the
 * product type are referenced by a positive id. The <em>existence</em> of that
 * category and product type is a cross-aggregate check the use case performs
 * against the repositories.</p>
 *
 * <p>{@code categoryName} / {@code productTypeName} are read-model labels filled by
 * the persistence layer; they never carry an invariant.</p>
 */
public final class Product {

    public static final int MAX_NAME_LENGTH = 200;

    private final long id;
    private final String name;
    private final String description;
    private final String imageBase;
    private final Money price;
    private final long categoryId;
    private final String categoryName;
    private final long productTypeId;
    private final String productTypeName;
    private final int stock;
    private final boolean active;

    public Product(long id, String name, String description, String imageBase, Money price,
                   long categoryId, String categoryName,
                   long productTypeId, String productTypeName,
                   int stock, boolean active) {
        String trimmedName = name == null ? "" : name.trim();
        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("product name is required");
        }
        if (trimmedName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "product name must not exceed " + MAX_NAME_LENGTH + " characters");
        }
        Objects.requireNonNull(price, "price is required");
        if (!price.isPositive()) {
            throw new IllegalArgumentException("price must be greater than 0");
        }
        if (stock < 0) {
            throw new IllegalArgumentException("stock must not be negative");
        }
        if (categoryId <= 0) {
            throw new IllegalArgumentException("a category must be referenced");
        }
        if (productTypeId <= 0) {
            throw new IllegalArgumentException("a product type must be referenced");
        }
        this.id = id;
        this.name = trimmedName;
        this.description = description == null ? "" : description;
        this.imageBase = imageBase == null ? "" : imageBase;
        this.price = price;
        this.categoryId = categoryId;
        this.categoryName = categoryName == null ? "" : categoryName;
        this.productTypeId = productTypeId;
        this.productTypeName = productTypeName == null ? "" : productTypeName;
        this.stock = stock;
        this.active = active;
    }

    /** A brand-new product with no id and no read-model labels yet. */
    public static Product create(String name, String description, String imageBase, Money price,
                                 long categoryId, long productTypeId, int stock, boolean active) {
        return new Product(0L, name, description, imageBase, price,
                categoryId, null, productTypeId, null, stock, active);
    }

    /** The same product with a different stock level (e.g. after an order decrements it). */
    public Product withStock(int newStock) {
        return new Product(id, name, description, imageBase, price,
                categoryId, categoryName, productTypeId, productTypeName, newStock, active);
    }

    public boolean hasStockFor(int units) {
        return stock >= units;
    }

    public long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public String imageBase() {
        return imageBase;
    }

    public Money price() {
        return price;
    }

    public long categoryId() {
        return categoryId;
    }

    public String categoryName() {
        return categoryName;
    }

    public long productTypeId() {
        return productTypeId;
    }

    public String productTypeName() {
        return productTypeName;
    }

    public int stock() {
        return stock;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Product product
                && product.id == this.id
                && product.name.equals(this.name)
                && product.price.equals(this.price)
                && product.categoryId == this.categoryId
                && product.productTypeId == this.productTypeId
                && product.stock == this.stock
                && product.active == this.active;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, price, categoryId, productTypeId, stock, active);
    }

    @Override
    public String toString() {
        return "Product{id=" + id + ", name='" + name + "', price=" + price
                + ", stock=" + stock + ", active=" + active + '}';
    }
}
