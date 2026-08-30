package com.unicornt.store.service;

import com.unicornt.store.domain.exception.DuplicateResourceException;
import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.model.Role;
import com.unicornt.store.model.User;
import com.unicornt.store.repository.RoleRepository;
import com.unicornt.store.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class UserServiceImpl implements UserService {

    /** Default role granted to every self-registered account. */
    public static final String DEFAULT_ROLE = "ROLE_USER";

    private static final int MIN_PASSWORD_LENGTH = 6;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User register(String firstName, String lastName, String email, String rawPassword) {
        requireText(firstName, "First name is required");
        requireText(lastName, "Last name is required");
        requireText(email, "Email is required");
        requireText(rawPassword, "Password is required");
        if (rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }
        if (emailExists(email)) {
            throw new DuplicateResourceException("User", "email", email);
        }

        Role defaultRole = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Role " + DEFAULT_ROLE + " not found. Run the data seed first."));

        User user = new User();
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setEmail(email.trim());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRoles(Set.of(defaultRole));

        return userRepository.save(user);
    }

    @Override
    public boolean emailExists(String email) {
        return email != null && userRepository.findByEmail(email.trim()).isPresent();
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
