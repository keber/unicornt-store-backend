package com.unicornt.store.application.usecase.ordering;

import com.unicornt.store.domain.model.Order;
import com.unicornt.store.domain.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/** The caller's own order history, most recent first. */
@Service
public class ListOrdersUseCase {

    private final OrderRepository orders;

    public ListOrdersUseCase(OrderRepository orders) {
        this.orders = orders;
    }

    public List<Order> execute(String userId) {
        return orders.findByUserId(userId);
    }
}
