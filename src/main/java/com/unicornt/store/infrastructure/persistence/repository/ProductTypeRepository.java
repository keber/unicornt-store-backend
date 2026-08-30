package com.unicornt.store.infrastructure.persistence.repository;

import com.unicornt.store.infrastructure.persistence.entity.ProductTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductTypeRepository extends JpaRepository<ProductTypeEntity, Integer> {

    List<ProductTypeEntity> findAllByOrderByIdAsc();

    Optional<ProductTypeEntity> findBySlug(String slug);
}
