package com.unicornt.store.domain.repository;

/**
 * Port for the inventory decrement that placing an order performs. Kept separate
 * from {@link ProductRepository} (the catalog port, owned by the catalog slice): a
 * single conditional {@code UPDATE ... WHERE stock >= :qty} is the check and the
 * decrement in one atomic step.
 */
public interface StockRepository {

    /**
     * Atomically decrements the stock of {@code productId} by {@code quantity},
     * only if that much is available.
     *
     * @return {@code true} when the stock was decremented, {@code false} when there was not enough
     */
    boolean decreaseStock(long productId, int quantity);
}
