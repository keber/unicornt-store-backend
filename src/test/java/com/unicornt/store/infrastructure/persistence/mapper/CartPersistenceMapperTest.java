package com.unicornt.store.infrastructure.persistence.mapper;

import com.unicornt.store.domain.model.Cart;
import com.unicornt.store.domain.model.CartItem;
import com.unicornt.store.infrastructure.persistence.entity.CartItemEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CartPersistenceMapper")
class CartPersistenceMapperTest {

    private static final String USER = "buyer@unicornt.dev";

    @Test
    @DisplayName("maps a single row to a cart line")
    void rowToItem() {
        CartItem item = CartPersistenceMapper.toDomain(new CartItemEntity(7L, 10, 3));

        assertThat(item.productId()).isEqualTo(10L);
        assertThat(item.quantity().value()).isEqualTo(3);
    }

    @Test
    @DisplayName("assembles the aggregate from the row list")
    void rowsToCart() {
        Cart cart = CartPersistenceMapper.toDomain(USER,
                List.of(new CartItemEntity(7L, 10, 2), new CartItemEntity(7L, 20, 1)));

        assertThat(cart.userId()).isEqualTo(USER);
        assertThat(cart.items()).containsExactly(CartItem.of(10, 2), CartItem.of(20, 1));
    }

    @Test
    @DisplayName("an empty row list yields an empty cart")
    void emptyRows() {
        assertThat(CartPersistenceMapper.toDomain(USER, List.of()).isEmpty()).isTrue();
    }

    @Test
    @DisplayName("maps a cart line to a new row owned by the numeric user id")
    void itemToRow() {
        CartItemEntity row = CartPersistenceMapper.toEntity(7L, CartItem.of(10, 4));

        assertThat(row.getUserId()).isEqualTo(7L);
        assertThat(row.getProductId()).isEqualTo(10);
        assertThat(row.getQuantity()).isEqualTo(4);
        assertThat(row.getId()).isNull();
    }
}
