package com.unicornt.store.infrastructure.persistence.mapper;

import com.unicornt.store.domain.model.User;
import com.unicornt.store.infrastructure.persistence.entity.RoleEntity;
import com.unicornt.store.infrastructure.persistence.entity.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserPersistenceMapper")
class UserPersistenceMapperTest {

    private static UserEntity row() {
        UserEntity entity = new UserEntity();
        entity.setId(7L);
        entity.setFirstName("Ada");
        entity.setLastName("Lovelace");
        entity.setEmail("ada@example.com");
        entity.setPassword("hashed-secret");
        entity.setRoles(Set.of(new RoleEntity("ROLE_USER"), new RoleEntity("ROLE_ADMIN")));
        return entity;
    }

    @Test
    @DisplayName("toDomain copies every scalar field and the role names")
    void toDomain() {
        User user = UserPersistenceMapper.toDomain(row());

        assertThat(user.id()).isEqualTo(7L);
        assertThat(user.firstName()).isEqualTo("Ada");
        assertThat(user.lastName()).isEqualTo("Lovelace");
        assertThat(user.email()).isEqualTo("ada@example.com");
        assertThat(user.passwordHash()).isEqualTo("hashed-secret");
        assertThat(user.roles()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("copyScalars writes the model fields onto the entity and leaves roles untouched")
    void copyScalars() {
        UserEntity target = new UserEntity();
        User user = new User(1L, "Grace", "Hopper", "grace@example.com", "hash", Set.of("ROLE_USER"));

        UserPersistenceMapper.copyScalars(user, target);

        assertThat(target.getFirstName()).isEqualTo("Grace");
        assertThat(target.getLastName()).isEqualTo("Hopper");
        assertThat(target.getEmail()).isEqualTo("grace@example.com");
        assertThat(target.getPassword()).isEqualTo("hash");
        assertThat(target.getRoles()).isEmpty();
    }
}
