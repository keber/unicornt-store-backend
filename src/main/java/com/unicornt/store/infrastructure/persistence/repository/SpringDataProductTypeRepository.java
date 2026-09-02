package com.unicornt.store.infrastructure.persistence.repository;

import com.unicornt.store.infrastructure.persistence.entity.ProductTypeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Spring Data repository for the {@code product_types} table. */
public interface SpringDataProductTypeRepository extends JpaRepository<ProductTypeJpaEntity, Integer> {

    List<ProductTypeJpaEntity> findAllByOrderByIdAsc();

    Optional<ProductTypeJpaEntity> findBySlug(String slug);
}
