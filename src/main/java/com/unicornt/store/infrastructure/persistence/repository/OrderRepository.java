package com.unicornt.store.infrastructure.persistence.repository;

import com.unicornt.store.infrastructure.persistence.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Ownership aware lookup: an order of another user must be indistinguishable from a missing one. */
    Optional<OrderEntity> findByIdAndUserId(Long id, Long userId);
}
