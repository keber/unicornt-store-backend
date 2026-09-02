package com.unicornt.store.infrastructure.persistence.adapter;

import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.domain.model.Order;
import com.unicornt.store.domain.repository.OrderRepository;
import com.unicornt.store.infrastructure.persistence.entity.OrderJpaEntity;
import com.unicornt.store.infrastructure.persistence.mapper.OrderPersistenceMapper;
import com.unicornt.store.infrastructure.persistence.repository.SpringDataOrderRepository;
import com.unicornt.store.infrastructure.persistence.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * JPA-backed {@link OrderRepository}. The domain keys orders by the principal
 * (email); the {@code orders.user_id} column is numeric, so this adapter resolves
 * the one to the other through the user repository at the boundary.
 */
@Component
public class OrderRepositoryAdapter implements OrderRepository {

    private final SpringDataOrderRepository orders;
    private final UserRepository users;

    public OrderRepositoryAdapter(SpringDataOrderRepository orders, UserRepository users) {
        this.orders = orders;
        this.users = users;
    }

    @Override
    public Order save(Order order) {
        long numericUserId = resolveUserId(order.userId());
        OrderJpaEntity saved = orders.save(OrderPersistenceMapper.toEntity(order, numericUserId));
        return OrderPersistenceMapper.toDomain(saved, order.userId());
    }

    @Override
    public Optional<Order> findByIdAndUserId(long id, String userId) {
        return orders.findByIdAndUserId(id, resolveUserId(userId))
                .map(entity -> OrderPersistenceMapper.toDomain(entity, userId));
    }

    @Override
    public List<Order> findByUserId(String userId) {
        return orders.findByUserIdOrderByCreatedAtDesc(resolveUserId(userId)).stream()
                .map(entity -> OrderPersistenceMapper.toDomain(entity, userId))
                .toList();
    }

    private long resolveUserId(String email) {
        return users.findByEmail(email)
                .map(user -> user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
    }
}
