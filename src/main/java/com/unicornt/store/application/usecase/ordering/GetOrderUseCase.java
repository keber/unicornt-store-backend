package com.unicornt.store.application.usecase.ordering;

import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.domain.model.Order;
import com.unicornt.store.domain.repository.OrderRepository;
import org.springframework.stereotype.Service;

/** Returns one order of the caller, or a not-found (an order of another user reads as missing). */
@Service
public class GetOrderUseCase {

    private final OrderRepository orders;

    public GetOrderUseCase(OrderRepository orders) {
        this.orders = orders;
    }

    public Order execute(long id, String userId) {
        return orders.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }
}
