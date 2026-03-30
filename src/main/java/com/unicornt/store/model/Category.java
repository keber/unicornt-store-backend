package com.unicornt.store.model;

import jakarta.persistence.*;

/**
 * Modelo de dominio para categorias de productos.
 * Refleja la tabla categories.
 */
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int    id;

    private String name;
    private String slug;

    public Category() {}

    public int    getId()   { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug != null ? slug : ""; }

    public void setId(int id)       { this.id = id; }
    public void setName(String name){ this.name = name; }
    public void setSlug(String slug){ this.slug = slug; }
}
