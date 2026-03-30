package com.unicornt.store.service;

import com.unicornt.store.dao.ProductDAO;
import com.unicornt.store.model.Product;
import com.unicornt.store.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductDAO productDAO;
    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductDAO productDAO, ProductRepository productRepository) {
        this.productDAO = productDAO;
        this.productRepository = productRepository;
    }

    @Override
    public List<Product> findAll(String nameFilter, Integer categoryId) {
        return productDAO.findAll(nameFilter, categoryId);
    }

    @Override
    public int countAll(String nameFilter, Integer categoryId) {
        return productDAO.countAll(nameFilter, categoryId);
    }

    @Override
    public List<Product> findAll(String nameFilter, Integer categoryId, int limit, int offset) {
        return productDAO.findAll(nameFilter, categoryId, limit, offset);
    }

    @Override
    public Product findById(int id) {
        return productDAO.findById(id);
    }

    @Override
    @Transactional
    public void insert(Product product) {
        productDAO.insert(product);
    }

    @Override
    @Transactional
    public void update(Product product) {
        productDAO.update(product);
    }

    @Override
    @Transactional
    public void delete(int id) {
        productDAO.delete(id);
    }
}
