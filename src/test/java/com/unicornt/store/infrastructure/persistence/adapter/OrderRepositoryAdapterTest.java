package com.unicornt.store.infrastructure.persistence.adapter;

import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.domain.model.Order;
import com.unicornt.store.domain.model.OrderItem;
import com.unicornt.store.domain.model.ShippingAddress;
import com.unicornt.store.domain.valueobject.Money;
import com.unicornt.store.infrastructure.persistence.entity.OrderJpaEntity;
import com.unicornt.store.infrastructure.persistence.entity.UserEntity;
import com.unicornt.store.infrastructure.persistence.mapper.OrderPersistenceMapper;
import com.unicornt.store.infrastructure.persistence.repository.SpringDataOrderRepository;
import com.unicornt.store.infrastructure.persistence.repository.UserRepository;
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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderRepositoryAdapter")
class OrderRepositoryAdapterTest {

    @Mock private SpringDataOrderRepository orders;
    @Mock private UserRepository users;
    @InjectMocks private OrderRepositoryAdapter adapter;

    private static Order domainOrder() {
        return Order.place("ada@example.com",
                new ShippingAddress("Av. 1234", "Santiago", "RM", null),
                List.of(new OrderItem(2L, "Mug", Money.ofClp(7990), 2)));
    }

    private static UserEntity user(long id) {
        UserEntity u = new UserEntity();
        u.setId(id);
        u.setEmail("ada@example.com");
        return u;
    }

    @Test
    @DisplayName("save resolves the principal to a numeric id, persists and maps back with the principal")
    void save() {
        when(users.findByEmail("ada@example.com")).thenReturn(Optional.of(user(42L)));
        when(orders.save(any(OrderJpaEntity.class))).thenAnswer(call -> {
            OrderJpaEntity e = call.getArgument(0);
            e.setId(58L);
            return e;
        });

        Order saved = adapter.save(domainOrder());

        ArgumentCaptor<OrderJpaEntity> captor = ArgumentCaptor.forClass(OrderJpaEntity.class);
        verify(orders).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(42L);
        assertThat(saved.id()).isEqualTo(58L);
        assertThat(saved.userId()).isEqualTo("ada@example.com");
    }

    @Test
    @DisplayName("save fails with not-found when the principal has no account")
    void saveUnknownUser() {
        when(users.findByEmail("ada@example.com")).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> adapter.save(domainOrder()));
    }

    @Test
    @DisplayName("findByIdAndUserId maps a hit and returns empty on a miss")
    void findById() {
        when(users.findByEmail("ada@example.com")).thenReturn(Optional.of(user(42L)));
        OrderJpaEntity row = OrderPersistenceMapper.toEntity(domainOrder(), 42L);
        row.setId(58L);
        when(orders.findByIdAndUserId(58L, 42L)).thenReturn(Optional.of(row));
        when(orders.findByIdAndUserId(99L, 42L)).thenReturn(Optional.empty());

        assertThat(adapter.findByIdAndUserId(58L, "ada@example.com"))
                .hasValueSatisfying(o -> assertThat(o.total()).isEqualTo(Money.ofClp(15980)));
        assertThat(adapter.findByIdAndUserId(99L, "ada@example.com")).isEmpty();
    }

    @Test
    @DisplayName("findByUserId maps the ordered list")
    void findByUserId() {
        when(users.findByEmail("ada@example.com")).thenReturn(Optional.of(user(42L)));
        OrderJpaEntity row = OrderPersistenceMapper.toEntity(domainOrder(), 42L);
        row.setId(58L);
        when(orders.findByUserIdOrderByCreatedAtDesc(42L)).thenReturn(List.of(row));

        assertThat(adapter.findByUserId("ada@example.com")).singleElement()
                .satisfies(o -> assertThat(o.id()).isEqualTo(58L));
    }
}
