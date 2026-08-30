package com.unicornt.store.domain.service;

import com.unicornt.store.infrastructure.persistence.entity.UserEntity;

/** Account use cases. */
public interface UserService {

    /**
     * Registers a new account with the ROLE_USER role and a hashed password.
     *
     * @throws com.unicornt.store.domain.exception.DuplicateResourceException if the email is taken
     */
    UserEntity register(String firstName, String lastName, String email, String rawPassword);

    boolean emailExists(String email);

    /**
     * Returns the account with the given email.
     *
     * @throws com.unicornt.store.domain.exception.ResourceNotFoundException if it does not exist
     */
    UserEntity findByEmail(String email);
}
