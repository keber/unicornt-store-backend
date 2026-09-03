package com.unicornt.store.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Catalog product row. The transient category and product-type names are filled by
 * the persistence mapper from a lookup; they are not columns of the products table.
 */
@Entity
@Table(name = "products")
public class ProductJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    @Column(name = "product_type_id")
    private int productTypeId;

    @Transient
    private String productTypeName;

    @Column(name = "category_id")
    private int categoryId;

    @Transient
    private String categoryName;

    private int price;
    private String description;

    @Column(name = "image_base")
    private String imageBase;

    @Column(name = "is_active")
    private boolean active;

    private int stock;

    public ProductJpaEntity() {
        // required no-arg constructor for JPA; rows are populated through the setters
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getProductTypeId() {
        return productTypeId;
    }

    public String getProductTypeName() {
        return productTypeName != null ? productTypeName : "";
    }

    public int getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName != null ? categoryName : "";
    }

    public int getPrice() {
        return price;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public String getImageBase() {
        return imageBase != null ? imageBase : "";
    }

    public boolean isActive() {
        return active;
    }

    public int getStock() {
        return stock;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProductTypeId(int productTypeId) {
        this.productTypeId = productTypeId;
    }

    public void setProductTypeName(String productTypeName) {
        this.productTypeName = productTypeName;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setImageBase(String imageBase) {
        this.imageBase = imageBase;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }
}
