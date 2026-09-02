package com.unicornt.store.domain.repository;

/** Port for the account-role reference data. */
public interface RoleRepository {

    /** Whether a role with this exact name has been seeded. */
    boolean existsByName(String name);
}
