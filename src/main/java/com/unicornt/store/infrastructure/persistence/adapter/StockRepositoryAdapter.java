package com.unicornt.store.infrastructure.persistence.adapter;

import com.unicornt.store.domain.repository.StockRepository;
import com.unicornt.store.infrastructure.persistence.repository.SpringDataStockRepository;
import org.springframework.stereotype.Component;

/** JPA-backed {@link StockRepository}: a single conditional {@code UPDATE} is check + decrement. */
@Component
public class StockRepositoryAdapter implements StockRepository {

    private final SpringDataStockRepository stock;

    public StockRepositoryAdapter(SpringDataStockRepository stock) {
        this.stock = stock;
    }

    @Override
    public boolean decreaseStock(long productId, int quantity) {
        return stock.decreaseStock((int) productId, quantity) == 1;
    }
}
