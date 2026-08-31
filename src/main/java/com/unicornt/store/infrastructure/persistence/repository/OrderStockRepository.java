package com.unicornt.store.infrastructure.persistence.repository;

import com.unicornt.store.infrastructure.persistence.entity.ProductEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Inventory access used by checkout. It reads and decrements the {@code products.stock}
 * column with native SQL so the catalog entity and repository, owned by the catalog slice,
 * stay untouched.
 */
public interface OrderStockRepository extends Repository<ProductEntity, Integer> {

    @Query(value = "SELECT stock FROM products WHERE id = :productId", nativeQuery = true)
    Optional<Integer> findStock(@Param("productId") int productId);

    /**
     * Decrements the stock of a product only if the requested quantity is available.
     *
     * @return 1 when the stock was decremented, 0 when there was not enough stock
     */
    @Modifying
    @Query(value = "UPDATE products SET stock = stock - :quantity "
                 + "WHERE id = :productId AND stock >= :quantity", nativeQuery = true)
    int decreaseStock(@Param("productId") int productId, @Param("quantity") int quantity);
}
