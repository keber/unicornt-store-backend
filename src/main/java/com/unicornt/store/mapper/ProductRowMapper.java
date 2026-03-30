package com.unicornt.store.mapper;

import com.unicornt.store.model.Product;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/** Mapea una fila del JOIN products/categories/product_types a un objeto Product. */
public class ProductRowMapper implements RowMapper<Product> {

    @Override
    public Product mapRow(ResultSet rs, int rowNum) throws SQLException {
        Product p = new Product();
        p.setId(rs.getInt("id"));
        p.setName(rs.getString("name"));
        p.setProductTypeId(rs.getInt("product_type_id"));
        p.setProductTypeName(rs.getString("product_type_name"));
        p.setCategoryId(rs.getInt("category_id"));
        p.setCategoryName(rs.getString("category_name"));
        p.setPrice(rs.getInt("price"));
        p.setDescription(rs.getString("description"));
        p.setImageBase(rs.getString("image_base"));
        p.setActive(rs.getInt("is_active") == 1);
        return p;
    }
}
