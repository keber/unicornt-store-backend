package com.unicornt.store.infrastructure.persistence.mapper;

import com.unicornt.store.domain.model.User;
import com.unicornt.store.infrastructure.persistence.entity.RoleEntity;
import com.unicornt.store.infrastructure.persistence.entity.UserEntity;

import java.util.LinkedHashSet;
import java.util.Set;

/** Converts between the {@link UserEntity} row and the {@link User} domain model. */
public final class UserPersistenceMapper {

    private UserPersistenceMapper() {
    }

    public static User toDomain(UserEntity entity) {
        Set<String> roleNames = new LinkedHashSet<>();
        for (RoleEntity role : entity.getRoles()) {
            roleNames.add(role.getName());
        }
        return new User(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getPassword(),
                roleNames);
    }

    /**
     * Copies the scalar fields of {@code user} onto {@code target}. Roles are resolved
     * against persisted {@link RoleEntity} rows by the repository adapter, so they are
     * not touched here.
     */
    public static void copyScalars(User user, UserEntity target) {
        target.setFirstName(user.firstName());
        target.setLastName(user.lastName());
        target.setEmail(user.email());
        target.setPassword(user.passwordHash());
    }
}
