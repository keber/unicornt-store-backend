package com.unicornt.store.service;

import com.unicornt.store.model.User;

/** Account use cases. */
public interface UserService {

    /**
     * Registers a new account with the ROLE_USER role and a hashed password.
     *
     * @throws com.unicornt.store.domain.exception.DuplicateResourceException if the email is taken
     */
    User register(String firstName, String lastName, String email, String rawPassword);

    boolean emailExists(String email);

    /**
     * Returns the account with the given email.
     *
     * @throws com.unicornt.store.domain.exception.ResourceNotFoundException if it does not exist
     */
    User findByEmail(String email);
}
