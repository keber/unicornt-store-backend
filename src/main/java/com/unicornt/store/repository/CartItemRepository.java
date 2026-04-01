package com.unicornt.store.repository;

import com.unicornt.store.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserId(Long userId);

    Optional<CartItem> findByUserIdAndProductId(Long userId, int productId);

    void deleteByUserId(Long userId);

    int countByUserId(Long userId);
}
