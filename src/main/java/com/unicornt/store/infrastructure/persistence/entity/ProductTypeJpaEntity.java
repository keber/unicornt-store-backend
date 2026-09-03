package com.unicornt.store.infrastructure.persistence.entity;

import jakarta.persistence.*;

/** Product type row (T-shirt, Mug, Poster). */
@Entity
@Table(name = "product_types")
public class ProductTypeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int    id;

    private String name;
    private String slug;

    public ProductTypeJpaEntity() {
        // required no-arg constructor for JPA; rows are populated through the setters
    }

    public int    getId()   { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug != null ? slug : ""; }

    public void setId(int id)       { this.id = id; }
    public void setName(String name){ this.name = name; }
    public void setSlug(String slug){ this.slug = slug; }
}
