package com.unicornt.store.service;

import com.unicornt.store.dto.RegisterRequest;
import com.unicornt.store.model.Role;
import com.unicornt.store.model.User;
import com.unicornt.store.repository.RoleRepository;
import com.unicornt.store.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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

    private Role clientRole;

    @BeforeEach
    void setUp() {
        clientRole = new Role();
        clientRole.setId(1L);
        clientRole.setName("ROLE_CLIENT");
    }

    @Test
    void register_debeCrearUsuarioConRolClient() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Juan");
        request.setLastName("Pérez");
        request.setEmail("juan@test.com");
        request.setPassword("password123");

        when(roleRepository.findByName("ROLE_CLIENT")).thenReturn(Optional.of(clientRole));
        when(passwordEncoder.encode("password123")).thenReturn("$2a$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        User result = userService.register(request);

        assertNotNull(result);
        assertEquals("Juan", result.getFirstName());
        assertEquals("juan@test.com", result.getEmail());
        assertEquals("$2a$encoded", result.getPassword());
        assertTrue(result.getRoles().contains(clientRole));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_debeLanzarExcepcionSiRolNoExiste() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Ana");
        request.setLastName("López");
        request.setEmail("ana@test.com");
        request.setPassword("abc123");

        when(roleRepository.findByName("ROLE_CLIENT")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.register(request));
        assertTrue(ex.getMessage().contains("ROLE_CLIENT"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void emailExists_debeRetornarTrueSiExiste() {
        when(userRepository.findByEmail("existe@test.com")).thenReturn(Optional.of(new User()));
        assertTrue(userService.emailExists("existe@test.com"));
    }

    @Test
    void emailExists_debeRetornarFalseSiNoExiste() {
        when(userRepository.findByEmail("nuevo@test.com")).thenReturn(Optional.empty());
        assertFalse(userService.emailExists("nuevo@test.com"));
    }
}
