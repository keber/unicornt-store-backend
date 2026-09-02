package com.unicornt.store.application.usecase.identity;

import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.domain.model.User;
import com.unicornt.store.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

/** Resolves the account behind an authenticated principal, or fails with a not-found. */
@Service
public class GetUserByEmailUseCase {

    private final UserRepository userRepository;

    public GetUserByEmailUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User execute(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
    }
}
