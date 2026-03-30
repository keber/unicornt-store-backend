package com.unicornt.store.service;

import com.unicornt.store.model.Product;

import java.util.List;

public interface ProductService {

    List<Product> findAll(String nameFilter, Integer categoryId);

    int countAll(String nameFilter, Integer categoryId);

    List<Product> findAll(String nameFilter, Integer categoryId, int limit, int offset);

    Product findById(int id);

    void insert(Product product);

    void update(Product product);

    void delete(int id);
}
