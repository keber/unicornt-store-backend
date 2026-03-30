package com.unicornt.store.mapper;

import com.unicornt.store.model.Category;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/** Mapea una fila de la tabla categories a un objeto Category. */
public class CategoryRowMapper implements RowMapper<Category> {

    @Override
    public Category mapRow(ResultSet rs, int rowNum) throws SQLException {
        Category c = new Category();
        c.setId(rs.getInt("id"));
        c.setName(rs.getString("name"));
        c.setSlug(rs.getString("slug"));
        return c;
    }
}
