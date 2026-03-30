package com.unicornt.store.mapper;

import com.unicornt.store.model.ProductType;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/** Mapea una fila de la tabla product_types a un objeto ProductType. */
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
