package com.unicornt.store.repository;

import com.unicornt.store.model.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductTypeRepository extends JpaRepository<ProductType, Integer> {

    List<ProductType> findAllByOrderByIdAsc();

    Optional<ProductType> findBySlug(String slug);
}
