package com.unicornt.store.infrastructure.persistence.adapter;

import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.domain.model.User;
import com.unicornt.store.domain.repository.UserRepository;
import com.unicornt.store.infrastructure.persistence.entity.RoleEntity;
import com.unicornt.store.infrastructure.persistence.entity.UserEntity;
import com.unicornt.store.infrastructure.persistence.mapper.UserPersistenceMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * JPA-backed implementation of the {@link UserRepository} domain port.
 *
 * <p>It delegates to the Spring Data
 * {@code infrastructure.persistence.repository.UserRepository} (referenced by its
 * fully qualified name to avoid the clash with the port above) and resolves role
 * names against the seeded {@link RoleEntity} rows at the boundary.</p>
 */
@Component
public class UserRepositoryAdapter implements UserRepository {

    private final com.unicornt.store.infrastructure.persistence.repository.UserRepository users;
    private final com.unicornt.store.infrastructure.persistence.repository.RoleRepository roles;

    public UserRepositoryAdapter(
            com.unicornt.store.infrastructure.persistence.repository.UserRepository users,
            com.unicornt.store.infrastructure.persistence.repository.RoleRepository roles) {
        this.users = users;
        this.roles = roles;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return users.findByEmail(email).map(UserPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return users.findByEmail(email).isPresent();
    }

    @Override
    public User save(User user) {
        UserEntity entity = user.id() == null
                ? new UserEntity()
                : users.findById(user.id())
                        .orElseThrow(() -> new ResourceNotFoundException("User", user.id()));
        UserPersistenceMapper.copyScalars(user, entity);
        entity.setRoles(resolveRoles(user.roles()));
        return UserPersistenceMapper.toDomain(users.save(entity));
    }

    private Set<RoleEntity> resolveRoles(Set<String> roleNames) {
        Set<RoleEntity> resolved = new LinkedHashSet<>();
        for (String name : roleNames) {
            resolved.add(roles.findByName(name)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Role " + name + " not found. Run the reference-data seed first.")));
        }
        return resolved;
    }
}
