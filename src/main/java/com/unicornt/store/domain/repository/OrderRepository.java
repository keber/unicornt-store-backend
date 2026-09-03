package com.unicornt.store.domain.repository;

import com.unicornt.store.domain.model.Order;

import java.util.List;
import java.util.Optional;

/** Port for order persistence. {@code userId} is the principal identity (email). */
public interface OrderRepository {

    Order save(Order order);

    /** Ownership-aware: an order of another user is indistinguishable from a missing one. */
    Optional<Order> findByIdAndUserId(long id, String userId);

    /** Orders of the given user, most recent first. */
    List<Order> findByUserId(String userId);
}
