package com.unicornt.store.model;

import jakarta.persistence.*;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Modelo de dominio para productos del catalogo.
 * Refleja la tabla products + JOIN con categories y product_types.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int    id;

    private String name;

    @Column(name = "product_type_id")
    private int    productTypeId;

    @Transient
    private String productTypeName;

    @Column(name = "category_id")
    private int    categoryId;

    @Transient
    private String categoryName;

    private int    price;
    private String description;

    @Column(name = "image_base")
    private String imageBase;

    @Column(name = "is_active")
    private boolean active;

    public Product() {}

    // ----------------------------------------------------------------
    // Getters
    // ----------------------------------------------------------------

    public int getId()                 { return id; }
    public String getName()            { return name; }
    public int getProductTypeId()      { return productTypeId; }
    public String getProductTypeName() { return productTypeName != null ? productTypeName : ""; }
    public int getCategoryId()         { return categoryId; }
    public String getCategoryName()    { return categoryName != null ? categoryName : ""; }
    public int getPrice()              { return price; }
    public String getDescription()     { return description != null ? description : ""; }
    public String getImageBase()       { return imageBase != null ? imageBase : ""; }
    public boolean isActive()          { return active; }

    /** Precio formateado en CLP para mostrar en vistas. */
    public String getFormattedPrice() {
        NumberFormat nf = NumberFormat.getInstance(Locale.of("es", "CL"));
        return "$" + nf.format(price);
    }

    // ----------------------------------------------------------------
    // Setters
    // ----------------------------------------------------------------

    public void setId(int id)                         { this.id = id; }
    public void setName(String name)                  { this.name = name; }
    public void setProductTypeId(int productTypeId)   { this.productTypeId = productTypeId; }
    public void setProductTypeName(String n)          { this.productTypeName = n; }
    public void setCategoryId(int categoryId)         { this.categoryId = categoryId; }
    public void setCategoryName(String categoryName)  { this.categoryName = categoryName; }
    public void setPrice(int price)                   { this.price = price; }
    public void setDescription(String description)    { this.description = description; }
    public void setImageBase(String imageBase)        { this.imageBase = imageBase; }
    public void setActive(boolean active)             { this.active = active; }
}
