package com.unicornt.store.domain.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A registered account.
 *
 * <p>Plain Java. Invariants enforced on construction (PLAN.md &sect;2.4): the first
 * and last name are required and at most {@value #MAX_NAME_LENGTH} characters, the
 * email is required and at most {@value #MAX_EMAIL_LENGTH} characters, the password
 * is stored only as a hash and is never blank, and the account carries at least one
 * non-blank role name.</p>
 *
 * <p>Two rules live outside this model on purpose: the <em>raw</em> password length
 * ({@code >= 6}) is checked by the registration use case before hashing, because the
 * model only ever sees the hash; and email <em>uniqueness</em> is a cross-account
 * check the use case performs against the repository.</p>
 */
public final class User {

    /** Default authority granted to every self-registered account. */
    public static final String ROLE_USER = "ROLE_USER";

    /** Authority that unlocks the administrative endpoints. */
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_EMAIL_LENGTH = 150;

    private final Long id;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String passwordHash;
    private final Set<String> roles;

    public User(Long id, String firstName, String lastName, String email,
                String passwordHash, Set<String> roles) {
        String trimmedFirstName = requireText(firstName, "first name is required");
        String trimmedLastName = requireText(lastName, "last name is required");
        String trimmedEmail = requireText(email, "email is required");
        if (trimmedFirstName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "first name must not exceed " + MAX_NAME_LENGTH + " characters");
        }
        if (trimmedLastName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "last name must not exceed " + MAX_NAME_LENGTH + " characters");
        }
        if (trimmedEmail.length() > MAX_EMAIL_LENGTH) {
            throw new IllegalArgumentException(
                    "email must not exceed " + MAX_EMAIL_LENGTH + " characters");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("password hash is required");
        }
        Objects.requireNonNull(roles, "roles are required");
        Set<String> copy = new LinkedHashSet<>();
        for (String role : roles) {
            copy.add(requireText(role, "a role name must not be blank"));
        }
        if (copy.isEmpty()) {
            throw new IllegalArgumentException("an account must have at least one role");
        }
        this.id = id;
        this.firstName = trimmedFirstName;
        this.lastName = trimmedLastName;
        this.email = trimmedEmail;
        this.passwordHash = passwordHash;
        this.roles = Collections.unmodifiableSet(copy);
    }

    /** A brand-new account with no id yet and an already-hashed password. */
    public static User register(String firstName, String lastName, String email,
                                String passwordHash, Set<String> roles) {
        return new User(null, firstName, lastName, email, passwordHash, roles);
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public Long id() {
        return id;
    }

    public String firstName() {
        return firstName;
    }

    public String lastName() {
        return lastName;
    }

    public String email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    /** The granted authorities, prefixed with {@code ROLE_}; unmodifiable. */
    public Set<String> roles() {
        return roles;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof User user
                && Objects.equals(user.id, this.id)
                && user.email.equals(this.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email);
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", email='" + email + "', roles=" + roles + '}';
    }
}
