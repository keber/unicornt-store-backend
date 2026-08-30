package com.unicornt.store.dao;

import com.unicornt.store.mapper.ProductTypeRowMapper;
import com.unicornt.store.model.ProductType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Data access for the product_types table using Spring JdbcTemplate. */
@Repository
public class ProductTypeDAO {

    private final JdbcTemplate jdbc;

    public ProductTypeDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Lists all product types ordered by id. */
    public List<ProductType> findAll() {
        return jdbc.query(
            "SELECT id, name, slug FROM product_types ORDER BY id",
            new ProductTypeRowMapper());
    }
}
