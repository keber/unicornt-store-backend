package com.unicornt.store.dao;

import com.unicornt.store.mapper.ProductTypeRowMapper;
import com.unicornt.store.model.ProductType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Acceso a datos para la tabla product_types usando Spring JdbcTemplate. */
@Repository
public class ProductTypeDAO {

    private final JdbcTemplate jdbc;

    public ProductTypeDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Lista todos los tipos de producto ordenados por id. */
    public List<ProductType> findAll() {
        return jdbc.query(
            "SELECT id, name, slug FROM product_types ORDER BY id",
            new ProductTypeRowMapper());
    }
}
