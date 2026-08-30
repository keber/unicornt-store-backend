package com.unicornt.store.repository;

import com.unicornt.store.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    List<Product> findByNameContainingIgnoreCase(String name, Sort sort);

    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    long countByNameContainingIgnoreCase(String name);

    List<Product> findByNameContainingIgnoreCaseAndCategoryId(String name, int categoryId, Sort sort);

    Page<Product> findByNameContainingIgnoreCaseAndCategoryId(String name, int categoryId, Pageable pageable);

    long countByNameContainingIgnoreCaseAndCategoryId(String name, int categoryId);
}
