package com.unicornt.store.infrastructure.persistence.repository;

import com.unicornt.store.infrastructure.persistence.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<ProductEntity, Integer> {

    List<ProductEntity> findByNameContainingIgnoreCase(String name, Sort sort);

    Page<ProductEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);

    long countByNameContainingIgnoreCase(String name);

    List<ProductEntity> findByNameContainingIgnoreCaseAndCategoryId(String name, int categoryId, Sort sort);

    Page<ProductEntity> findByNameContainingIgnoreCaseAndCategoryId(String name, int categoryId, Pageable pageable);

    long countByNameContainingIgnoreCaseAndCategoryId(String name, int categoryId);
}
