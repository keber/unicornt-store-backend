package com.unicornt.store.dao;

import com.unicornt.store.mapper.ProductRowMapper;
import com.unicornt.store.model.Product;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos para la tabla products usando Spring JdbcTemplate.
 */
@Repository
public class ProductDAO {

    private static final String BASE_SELECT =
        "SELECT p.id, p.name, p.product_type_id, pt.name AS product_type_name, " +
        "       p.category_id, c.name AS category_name, " +
        "       p.price, p.description, p.image_base, p.is_active " +
        "FROM   products p " +
        "INNER JOIN categories    c  ON p.category_id     = c.id " +
        "INNER JOIN product_types pt ON p.product_type_id = pt.id ";

    private final JdbcTemplate jdbc;
    private final ProductRowMapper rowMapper = new ProductRowMapper();

    public ProductDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ----------------------------------------------------------------
    // Consultas
    // ----------------------------------------------------------------

    public List<Product> findAll(String nameFilter, Integer categoryId) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder(BASE_SELECT).append("WHERE 1=1");
        appendFilters(sql, params, nameFilter, categoryId);
        sql.append(" ORDER BY p.id");
        return jdbc.query(sql.toString(), rowMapper, params.toArray());
    }

    public int countAll(String nameFilter, Integer categoryId) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM products p WHERE 1=1");
        appendFilters(sql, params, nameFilter, categoryId);
        Integer count = jdbc.queryForObject(sql.toString(), Integer.class, params.toArray());
        return count != null ? count : 0;
    }

    public List<Product> findAll(String nameFilter, Integer categoryId, int limit, int offset) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder(BASE_SELECT).append("WHERE 1=1");
        appendFilters(sql, params, nameFilter, categoryId);
        sql.append(" ORDER BY p.id LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);
        return jdbc.query(sql.toString(), rowMapper, params.toArray());
    }

    public Product findById(int id) {
        String sql = BASE_SELECT + "WHERE p.id = ?";
        List<Product> list = jdbc.query(sql, rowMapper, id);
        return list.isEmpty() ? null : list.get(0);
    }

    // ----------------------------------------------------------------
    // Mutaciones
    // ----------------------------------------------------------------

    public void insert(Product p) {
        String sql =
            "INSERT INTO products (name, product_type_id, category_id, price, description, image_base, is_active) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
        jdbc.update(sql,
            p.getName(),
            p.getProductTypeId(),
            p.getCategoryId(),
            p.getPrice(),
            emptyToNull(p.getDescription()),
            emptyToNull(p.getImageBase()),
            p.isActive() ? 1 : 0);
    }

    public void update(Product p) {
        String sql =
            "UPDATE products " +
            "SET name=?, product_type_id=?, category_id=?, price=?, description=?, image_base=?, is_active=? " +
            "WHERE id=?";
        jdbc.update(sql,
            p.getName(),
            p.getProductTypeId(),
            p.getCategoryId(),
            p.getPrice(),
            emptyToNull(p.getDescription()),
            emptyToNull(p.getImageBase()),
            p.isActive() ? 1 : 0,
            p.getId());
    }

    public void delete(int id) {
        jdbc.update("DELETE FROM products WHERE id = ?", id);
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private void appendFilters(StringBuilder sql, List<Object> params,
                               String nameFilter, Integer categoryId) {
        if (nameFilter != null && !nameFilter.trim().isEmpty()) {
            sql.append(" AND p.name LIKE ?");
            params.add("%" + nameFilter.trim() + "%");
        }
        if (categoryId != null && categoryId > 0) {
            sql.append(" AND p.category_id = ?");
            params.add(categoryId);
        }
    }

    private String emptyToNull(String value) {
        return (value == null || value.isEmpty()) ? null : value;
    }
}
