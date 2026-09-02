package com.unicornt.store.application.usecase.identity;

import com.unicornt.store.domain.exception.DuplicateResourceException;
import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.domain.model.User;
import com.unicornt.store.domain.repository.RoleRepository;
import com.unicornt.store.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Registers a self-service account: validate the input, enforce the raw-password
 * length and email-uniqueness rules (PLAN.md &sect;2.4), hash the password and
 * persist the account with the single {@link User#ROLE_USER} authority.
 */
@Service
public class RegisterUserUseCase {

    /** Minimum length of the raw password, checked here because the model only sees the hash. */
    public static final int MIN_PASSWORD_LENGTH = 6;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordHasher passwordHasher;

    public RegisterUserUseCase(UserRepository userRepository,
                               RoleRepository roleRepository,
                               PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordHasher = passwordHasher;
    }

    @Transactional
    public User execute(String firstName, String lastName, String email, String rawPassword) {
        requireText(firstName, "First name is required");
        requireText(lastName, "Last name is required");
        requireText(email, "Email is required");
        requireText(rawPassword, "Password is required");
        if (rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }

        String normalizedEmail = email.trim();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("User", "email", normalizedEmail);
        }
        if (!roleRepository.existsByName(User.ROLE_USER)) {
            throw new ResourceNotFoundException(
                    "Role " + User.ROLE_USER + " not found. Run the reference-data seed first.");
        }

        User account = User.register(
                firstName, lastName, normalizedEmail,
                passwordHasher.hash(rawPassword), Set.of(User.ROLE_USER));
        return userRepository.save(account);
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
