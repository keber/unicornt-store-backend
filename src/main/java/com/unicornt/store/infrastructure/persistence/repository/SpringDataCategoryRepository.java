package com.unicornt.store.infrastructure.persistence.repository;

import com.unicornt.store.infrastructure.persistence.entity.CategoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Spring Data repository for the {@code categories} table. */
public interface SpringDataCategoryRepository extends JpaRepository<CategoryJpaEntity, Integer> {

    List<CategoryJpaEntity> findAllByOrderByNameAsc();

    Optional<CategoryJpaEntity> findBySlug(String slug);
}
