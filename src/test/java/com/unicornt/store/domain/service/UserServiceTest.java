package com.unicornt.store.domain.service;

import com.unicornt.store.domain.exception.DuplicateResourceException;
import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.infrastructure.persistence.entity.RoleEntity;
import com.unicornt.store.infrastructure.persistence.entity.UserEntity;
import com.unicornt.store.infrastructure.persistence.repository.RoleRepository;
import com.unicornt.store.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private RoleEntity userRole;

    @BeforeEach
    void setUp() {
        userRole = new RoleEntity();
        userRole.setId(1L);
        userRole.setName(UserServiceImpl.DEFAULT_ROLE);
    }

    @Test
    void registerCreatesAccountWithDefaultRole() {
        when(userRepository.findByEmail("juan@test.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName(UserServiceImpl.DEFAULT_ROLE)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("$2a$encoded");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        UserEntity result = userService.register("Juan", "Perez", "juan@test.com", "password123");

        assertNotNull(result);
        assertEquals("Juan", result.getFirstName());
        assertEquals("juan@test.com", result.getEmail());
        assertEquals("$2a$encoded", result.getPassword());
        assertTrue(result.getRoles().contains(userRole));
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void registerFailsWhenTheDefaultRoleIsMissing() {
        when(userRepository.findByEmail("ana@test.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName(UserServiceImpl.DEFAULT_ROLE)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> userService.register("Ana", "Lopez", "ana@test.com", "abc123"));

        assertTrue(ex.getMessage().contains(UserServiceImpl.DEFAULT_ROLE));
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerRejectsAnAlreadyUsedEmail() {
        when(userRepository.findByEmail("taken@test.com")).thenReturn(Optional.of(new UserEntity()));

        assertThrows(DuplicateResourceException.class,
                () -> userService.register("Ana", "Lopez", "taken@test.com", "abc123"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void registerRejectsAShortPassword() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.register("Ana", "Lopez", "ana@test.com", "abc"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void emailExistsReturnsTrueForAKnownAccount() {
        when(userRepository.findByEmail("known@test.com")).thenReturn(Optional.of(new UserEntity()));
        assertTrue(userService.emailExists("known@test.com"));
    }

    @Test
    void emailExistsReturnsFalseForAnUnknownAccount() {
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        assertFalse(userService.emailExists("new@test.com"));
    }

    @Test
    void findByEmailFailsForAnUnknownAccount() {
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.findByEmail("ghost@test.com"));
    }
}
