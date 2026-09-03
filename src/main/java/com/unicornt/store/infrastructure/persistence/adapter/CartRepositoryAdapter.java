package com.unicornt.store.infrastructure.persistence.adapter;

import com.unicornt.store.domain.model.Cart;
import com.unicornt.store.domain.model.CartItem;
import com.unicornt.store.domain.repository.CartRepository;
import com.unicornt.store.infrastructure.persistence.entity.CartItemJpaEntity;
import com.unicornt.store.infrastructure.persistence.mapper.CartPersistenceMapper;
import com.unicornt.store.infrastructure.persistence.repository.SpringDataCartItemRepository;
import com.unicornt.store.infrastructure.persistence.repository.SpringDataUserRepository;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * JPA-backed implementation of the {@link CartRepository} port.
 *
 * <p>The port addresses a cart by the principal identity ({@code userId}, an
 * email). The {@code cart_items} table keys on the numeric user id, so this
 * adapter resolves the email to that id through {@link SpringDataUserRepository}; that
 * translation is the only reason the port is not numeric. When the identity
 * cannot be resolved the cart is treated as empty.</p>
 */
@Component
public class CartRepositoryAdapter implements CartRepository {

    private final SpringDataCartItemRepository rows;
    private final SpringDataUserRepository users;

    public CartRepositoryAdapter(SpringDataCartItemRepository rows, SpringDataUserRepository users) {
        this.rows = rows;
        this.users = users;
    }

    @Override
    public Cart findByUserId(String userId) {
        Long numericId = resolve(userId);
        if (numericId == null) {
            return Cart.empty(userId);
        }
        return CartPersistenceMapper.toDomain(userId, rows.findByUserId(numericId));
    }

    @Override
    public Cart save(Cart cart) {
        Long numericId = resolve(cart.userId());
        if (numericId == null) {
            throw new IllegalStateException("Cannot persist a cart for an unknown user: " + cart.userId());
        }

        Map<Integer, CartItemJpaEntity> current = new HashMap<>();
        for (CartItemJpaEntity row : rows.findByUserId(numericId)) {
            current.put(row.getProductId(), row);
        }

        for (CartItem item : cart.items()) {
            int productId = (int) item.productId();
            int quantity = item.quantity().value();
            CartItemJpaEntity existing = current.remove(productId);
            if (existing == null) {
                rows.save(CartPersistenceMapper.toEntity(numericId, item));
            } else if (existing.getQuantity() != quantity) {
                existing.setQuantity(quantity);
                rows.save(existing);
            }
        }
        current.values().forEach(rows::delete);

        return CartPersistenceMapper.toDomain(cart.userId(), rows.findByUserId(numericId));
    }

    @Override
    public void deleteByUserId(String userId) {
        Long numericId = resolve(userId);
        if (numericId != null) {
            rows.deleteByUserId(numericId);
        }
    }

    private Long resolve(String userId) {
        return users.findByEmail(userId).map(user -> user.getId()).orElse(null);
    }
}
