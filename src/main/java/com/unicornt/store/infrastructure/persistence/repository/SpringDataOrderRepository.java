package com.unicornt.store.infrastructure.persistence.repository;

import com.unicornt.store.infrastructure.persistence.entity.OrderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Spring Data repository for the {@code orders} table. */
public interface SpringDataOrderRepository extends JpaRepository<OrderJpaEntity, Long> {

    List<OrderJpaEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<OrderJpaEntity> findByIdAndUserId(Long id, Long userId);
}
