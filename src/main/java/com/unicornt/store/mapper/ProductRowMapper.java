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
        p.setActive(readBooleanCompat(rs, "is_active"));
        return p;
    }

    private boolean readBooleanCompat(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);

        if (value == null) {
            return false;
        }

        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }

        if (value instanceof byte[]) {
            String s = new String((byte[]) value).trim().toLowerCase();
            return parseBooleanString(column, s);
        }

        String s = value.toString().trim().toLowerCase();
        return parseBooleanString(column, s);
    }

    private boolean parseBooleanString(String column, String s) throws SQLException {
        switch (s) {
            case "1":
            case "true":
            case "t":
            case "yes":
            case "y":
                return true;

            case "0":
            case "false":
            case "f":
            case "no":
            case "n":
                return false;

            default:
                throw new SQLException("No se pudo interpretar como boolean la columna '" + column + "' con valor: " + s);
        }
    }
}

