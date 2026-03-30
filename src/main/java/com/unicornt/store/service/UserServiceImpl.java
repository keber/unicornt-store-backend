package com.unicornt.store.service;

import com.unicornt.store.dto.RegisterRequest;
import com.unicornt.store.model.Role;
import com.unicornt.store.model.User;
import com.unicornt.store.repository.RoleRepository;
import com.unicornt.store.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class UserServiceImpl implements UserService {

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
    public User register(RegisterRequest request) {
        Role clientRole = roleRepository.findByName("ROLE_CLIENT")
                .orElseThrow(() -> new RuntimeException("Rol ROLE_CLIENT no encontrado. Ejecuta el seed de datos."));

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(Set.of(clientRole));

        return userRepository.save(user);
    }

    @Override
    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
}
