package com.unicornt.store.infrastructure.persistence.adapter;

import com.unicornt.store.infrastructure.persistence.entity.RoleEntity;
import com.unicornt.store.infrastructure.persistence.repository.SpringDataRoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleRepositoryAdapter")
class RoleRepositoryAdapterTest {

    @Mock
    private SpringDataRoleRepository roles;
    @InjectMocks
    private RoleRepositoryAdapter adapter;

    @Test
    @DisplayName("existsByName reflects whether the seeded row is present")
    void existsByName() {
        when(roles.findByName("ROLE_USER")).thenReturn(Optional.of(new RoleEntity("ROLE_USER")));
        when(roles.findByName("ROLE_GHOST")).thenReturn(Optional.empty());

        assertThat(adapter.existsByName("ROLE_USER")).isTrue();
        assertThat(adapter.existsByName("ROLE_GHOST")).isFalse();
    }
}
