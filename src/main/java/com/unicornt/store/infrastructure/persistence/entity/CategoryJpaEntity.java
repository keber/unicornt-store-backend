package com.unicornt.store.infrastructure.persistence.entity;

import jakarta.persistence.*;

/** Product category row. */
@Entity
@Table(name = "categories")
public class CategoryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int    id;

    private String name;
    private String slug;

    public CategoryJpaEntity() {}

    public int    getId()   { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug != null ? slug : ""; }

    public void setId(int id)       { this.id = id; }
    public void setName(String name){ this.name = name; }
    public void setSlug(String slug){ this.slug = slug; }
}
