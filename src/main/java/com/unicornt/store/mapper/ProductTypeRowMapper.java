package com.unicornt.store.mapper;

import com.unicornt.store.model.ProductType;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/** Maps a row of the product_types table to a ProductType object. */
public class ProductTypeRowMapper implements RowMapper<ProductType> {

    @Override
    public ProductType mapRow(ResultSet rs, int rowNum) throws SQLException {
        ProductType pt = new ProductType();
        pt.setId(rs.getInt("id"));
        pt.setName(rs.getString("name"));
        pt.setSlug(rs.getString("slug"));
        return pt;
    }
}
