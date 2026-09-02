package com.unicornt.store.infrastructure.persistence.repository;

import com.unicornt.store.infrastructure.persistence.entity.CartItemJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SpringDataCartItemRepository extends JpaRepository<CartItemJpaEntity, Long> {

    List<CartItemJpaEntity> findByUserId(Long userId);

    Optional<CartItemJpaEntity> findByUserIdAndProductId(Long userId, int productId);

    void deleteByUserId(Long userId);

    int countByUserId(Long userId);

    @Query("SELECT COALESCE(SUM(c.quantity), 0) FROM CartItemJpaEntity c WHERE c.userId = ?1")
    int sumQuantityByUserId(Long userId);
}
