package com.unicornt.store.domain.repository;

import com.unicornt.store.domain.model.User;

import java.util.Optional;

/** Port for account persistence. Pure domain types in and out. */
public interface UserRepository {

    /** The account registered with this email, if any. */
    Optional<User> findByEmail(String email);

    /** Whether an account already exists for this email. */
    boolean existsByEmail(String email);

    /**
     * Persists a new or existing account and returns it with its generated id.
     * The role names carried by the account must already exist as reference data.
     */
    User save(User user);
}
