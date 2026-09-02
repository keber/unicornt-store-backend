package com.unicornt.store.application.usecase.identity;

/**
 * Port for one-way password hashing. The infrastructure adapter wraps the Spring
 * Security {@code PasswordEncoder}; keeping it behind this interface lets the
 * registration use case stay free of the framework.
 */
public interface PasswordHasher {

    /** Returns an irreversible hash of the given raw password. */
    String hash(String rawPassword);
}
