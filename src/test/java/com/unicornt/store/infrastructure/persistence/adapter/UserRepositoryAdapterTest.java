package com.unicornt.store.infrastructure.persistence.adapter;

import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.domain.model.User;
import com.unicornt.store.infrastructure.persistence.entity.RoleEntity;
import com.unicornt.store.infrastructure.persistence.entity.UserEntity;
import com.unicornt.store.infrastructure.persistence.repository.SpringDataRoleRepository;
import com.unicornt.store.infrastructure.persistence.repository.SpringDataUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRepositoryAdapter")
class UserRepositoryAdapterTest {

    @Mock
    private SpringDataUserRepository users;
    @Mock
    private SpringDataRoleRepository roles;
    @InjectMocks
    private UserRepositoryAdapter adapter;

    private static UserEntity row() {
        UserEntity entity = new UserEntity();
        entity.setId(3L);
        entity.setFirstName("Ada");
        entity.setLastName("Lovelace");
        entity.setEmail("ada@example.com");
        entity.setPassword("hashed");
        entity.setRoles(Set.of(new RoleEntity("ROLE_USER")));
        return entity;
    }

    @Test
    @DisplayName("findByEmail maps the matched row and returns empty when absent")
    void findByEmail() {
        when(users.findByEmail("ada@example.com")).thenReturn(Optional.of(row()));
        when(users.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThat(adapter.findByEmail("ada@example.com"))
                .hasValueSatisfying(u -> assertThat(u.email()).isEqualTo("ada@example.com"));
        assertThat(adapter.findByEmail("ghost@example.com")).isEmpty();
    }

    @Test
    @DisplayName("existsByEmail is true only when a row is present")
    void existsByEmail() {
        when(users.findByEmail("ada@example.com")).thenReturn(Optional.of(row()));
        when(users.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThat(adapter.existsByEmail("ada@example.com")).isTrue();
        assertThat(adapter.existsByEmail("ghost@example.com")).isFalse();
    }

    @Test
    @DisplayName("save resolves role names to seeded rows and persists a new entity")
    void savesNewAccount() {
        RoleEntity userRole = new RoleEntity("ROLE_USER");
        when(roles.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(users.save(any(UserEntity.class))).thenAnswer(call -> {
            UserEntity e = call.getArgument(0);
            e.setId(42L);
            return e;
        });

        User saved = adapter.save(User.register("Ada", "Lovelace", "ada@example.com", "hashed", Set.of("ROLE_USER")));

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(users).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("ada@example.com");
        assertThat(captor.getValue().getRoles()).containsExactly(userRole);
        assertThat(saved.id()).isEqualTo(42L);
    }

    @Test
    @DisplayName("save fails when a role name has not been seeded")
    void failsOnUnknownRole() {
        when(roles.findByName("ROLE_USER")).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() ->
                adapter.save(User.register("Ada", "Lovelace", "ada@example.com", "hashed", Set.of("ROLE_USER"))));
    }

    @Test
    @DisplayName("save fails when the referenced existing account id is gone")
    void failsOnMissingExistingAccount() {
        when(users.findById(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() ->
                adapter.save(new User(99L, "Ada", "Lovelace", "ada@example.com", "hashed", Set.of("ROLE_USER"))));
    }
}
