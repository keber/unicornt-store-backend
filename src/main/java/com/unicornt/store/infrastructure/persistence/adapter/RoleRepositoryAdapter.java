package com.unicornt.store.infrastructure.persistence.adapter;

import com.unicornt.store.domain.repository.RoleRepository;
import org.springframework.stereotype.Component;

/**
 * JPA-backed implementation of the {@link RoleRepository} domain port. It delegates
 * to the Spring Data {@code infrastructure.persistence.repository.SpringDataRoleRepository}
 * (referenced by its fully qualified name to avoid the clash with the port above).
 */
@Component
public class RoleRepositoryAdapter implements RoleRepository {

    private final com.unicornt.store.infrastructure.persistence.repository.SpringDataRoleRepository roles;

    public RoleRepositoryAdapter(
            com.unicornt.store.infrastructure.persistence.repository.SpringDataRoleRepository roles) {
        this.roles = roles;
    }

    @Override
    public boolean existsByName(String name) {
        return roles.findByName(name).isPresent();
    }
}
