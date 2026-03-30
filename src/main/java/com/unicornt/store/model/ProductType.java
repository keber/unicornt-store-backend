package com.unicornt.store.model;

import jakarta.persistence.*;

/**
 * Modelo de dominio para tipos de producto (Polera, Tazon).
 * Refleja la tabla product_types.
 */
@Entity
@Table(name = "product_types")
public class ProductType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int    id;

    private String name;
    private String slug;

    public ProductType() {}

    public int    getId()   { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug != null ? slug : ""; }

    public void setId(int id)       { this.id = id; }
    public void setName(String name){ this.name = name; }
    public void setSlug(String slug){ this.slug = slug; }
}
