package com.unicornt.store.dao;

import com.unicornt.store.mapper.CategoryRowMapper;
import com.unicornt.store.model.Category;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Acceso a datos para la tabla categories usando Spring JdbcTemplate. */
@Repository
public class CategoryDAO {

    private final JdbcTemplate jdbc;

    public CategoryDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Lista todas las categorias ordenadas por nombre. */
    public List<Category> findAll() {
        return jdbc.query(
            "SELECT id, name, slug FROM categories ORDER BY name",
            new CategoryRowMapper());
    }
}
