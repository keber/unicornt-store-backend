package com.unicornt.store.infrastructure.persistence.mapper;

import com.unicornt.store.domain.model.Cart;
import com.unicornt.store.domain.model.CartItem;
import com.unicornt.store.domain.valueobject.Quantity;
import com.unicornt.store.infrastructure.persistence.entity.CartItemEntity;

import java.util.List;

/**
 * Converts between the {@code cart_items} rows and the {@link Cart} aggregate.
 * Pure static methods, no framework. The aggregate is assembled from the row list;
 * writing it back is a per-line concern handled by the repository adapter, which
 * owns the numeric user key and the row lifecycle.
 */
public final class CartPersistenceMapper {

    private CartPersistenceMapper() {
    }

    public static CartItem toDomain(CartItemEntity row) {
        return new CartItem(row.getProductId(), Quantity.of(row.getQuantity()));
    }

    /** Assembles the aggregate for {@code userId} from its rows (any order). */
    public static Cart toDomain(String userId, List<CartItemEntity> rows) {
        List<CartItem> items = rows.stream().map(CartPersistenceMapper::toDomain).toList();
        return new Cart(userId, items);
    }

    /** A new row for {@code item} owned by {@code numericUserId} (id 0 means "insert"). */
    public static CartItemEntity toEntity(long numericUserId, CartItem item) {
        return new CartItemEntity(numericUserId, (int) item.productId(), item.quantity().value());
    }
}
