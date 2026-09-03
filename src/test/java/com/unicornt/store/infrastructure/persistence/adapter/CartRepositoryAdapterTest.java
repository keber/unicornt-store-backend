package com.unicornt.store.infrastructure.persistence.adapter;

import com.unicornt.store.domain.model.Cart;
import com.unicornt.store.domain.model.CartItem;
import com.unicornt.store.infrastructure.persistence.entity.CartItemJpaEntity;
import com.unicornt.store.infrastructure.persistence.entity.UserEntity;
import com.unicornt.store.infrastructure.persistence.repository.SpringDataCartItemRepository;
import com.unicornt.store.infrastructure.persistence.repository.SpringDataUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartRepositoryAdapter")
class CartRepositoryAdapterTest {

    private static final String USER = "buyer@unicornt.dev";
    private static final long USER_ID = 7L;

    @Mock
    private SpringDataCartItemRepository rows;
    @Mock
    private SpringDataUserRepository users;
    @InjectMocks
    private CartRepositoryAdapter adapter;

    private static CartItemJpaEntity row(long id, int productId, int quantity) {
        CartItemJpaEntity entity = new CartItemJpaEntity(USER_ID, productId, quantity);
        entity.setId(id);
        return entity;
    }

    private void userExists() {
        UserEntity user = new UserEntity();
        user.setId(USER_ID);
        user.setEmail(USER);
        when(users.findByEmail(USER)).thenReturn(Optional.of(user));
    }

    @Test
    @DisplayName("findByUserId resolves the numeric id and assembles the aggregate from the rows")
    void findByUserIdAssembles() {
        userExists();
        when(rows.findByUserId(USER_ID)).thenReturn(List.of(row(1, 10, 2), row(2, 20, 1)));

        Cart cart = adapter.findByUserId(USER);

        assertThat(cart.userId()).isEqualTo(USER);
        assertThat(cart.items()).containsExactly(CartItem.of(10, 2), CartItem.of(20, 1));
    }

    @Test
    @DisplayName("findByUserId returns an empty cart when the user cannot be resolved")
    void findByUserIdUnknownUser() {
        when(users.findByEmail(USER)).thenReturn(Optional.empty());

        Cart cart = adapter.findByUserId(USER);

        assertThat(cart.isEmpty()).isTrue();
        verify(rows, never()).findByUserId(any());
    }

    @Test
    @DisplayName("save inserts new lines, updates changed ones and deletes the rest")
    void saveDiffsRows() {
        userExists();
        CartItemJpaEntity keep = row(1, 10, 2);
        CartItemJpaEntity change = row(2, 20, 1);
        CartItemJpaEntity drop = row(3, 30, 5);
        when(rows.findByUserId(USER_ID))
                .thenReturn(List.of(keep, change, drop))
                .thenReturn(List.of(row(1, 10, 2), row(2, 20, 4), row(4, 40, 1)));

        adapter.save(new Cart(USER, List.of(CartItem.of(10, 2), CartItem.of(20, 4), CartItem.of(40, 1))));

        assertThat(change.getQuantity()).isEqualTo(4);
        verify(rows).delete(drop);

        ArgumentCaptor<CartItemJpaEntity> inserted = ArgumentCaptor.forClass(CartItemJpaEntity.class);
        verify(rows, org.mockito.Mockito.atLeastOnce()).save(inserted.capture());
        assertThat(inserted.getAllValues()).anySatisfy(saved -> {
            assertThat(saved.getProductId()).isEqualTo(40);
            assertThat(saved.getQuantity()).isEqualTo(1);
        });
        // the unchanged line is not re-saved
        verify(rows, never()).save(keep);
    }

    @Test
    @DisplayName("save fails when the user behind the cart cannot be resolved")
    void saveUnknownUser() {
        when(users.findByEmail(USER)).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatIllegalStateException()
                .isThrownBy(() -> adapter.save(Cart.empty(USER)));

        verify(rows, never()).save(any(CartItemJpaEntity.class));
    }

    @Test
    @DisplayName("deleteByUserId resolves the id and clears the rows")
    void deleteByUserId() {
        userExists();

        adapter.deleteByUserId(USER);

        verify(rows).deleteByUserId(USER_ID);
    }

    @Test
    @DisplayName("deleteByUserId is a no-op when the user cannot be resolved")
    void deleteByUserIdUnknownUser() {
        when(users.findByEmail(USER)).thenReturn(Optional.empty());

        adapter.deleteByUserId(USER);

        verify(rows, never()).deleteByUserId(any());
    }
}
