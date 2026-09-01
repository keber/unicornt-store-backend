package com.unicornt.store.infrastructure.persistence.repository;

import com.unicornt.store.infrastructure.persistence.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Integer> {

    List<CategoryEntity> findAllByOrderByNameAsc();

    Optional<CategoryEntity> findBySlug(String slug);
}
