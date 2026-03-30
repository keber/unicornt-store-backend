package com.unicornt.store;

import com.unicornt.store.model.Role;
import com.unicornt.store.model.User;
import com.unicornt.store.repository.RoleRepository;
import com.unicornt.store.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

/**
 * Punto de entrada de la aplicación Spring Boot.
 * Extiende SpringBootServletInitializer para permitir el despliegue como WAR
 * en un Tomcat externo (10.1+).
 */
@SpringBootApplication
public class StoreApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(StoreApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(StoreApplication.class, args);
    }

    @Bean
    CommandLineRunner initData(RoleRepository roleRepository,
                               UserRepository userRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            // Crear roles si no existen
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN")));
            Role clientRole = roleRepository.findByName("ROLE_CLIENT")
                    .orElseGet(() -> roleRepository.save(new Role("ROLE_CLIENT")));

            // Crear usuario admin si no existe
            if (userRepository.findByEmail("admin@unicornt.cl").isEmpty()) {
                User admin = new User();
                admin.setFirstName("Admin");
                admin.setLastName("Store");
                admin.setEmail("admin@unicornt.cl");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRoles(Set.of(adminRole));
                userRepository.save(admin);
            }

            // Crear usuario cliente de demostración si no existe
            if (userRepository.findByEmail("cliente@unicornt.cl").isEmpty()) {
                User client = new User();
                client.setFirstName("Cliente");
                client.setLastName("Demo");
                client.setEmail("cliente@unicornt.cl");
                client.setPassword(passwordEncoder.encode("cliente123"));
                client.setRoles(Set.of(clientRole));
                userRepository.save(client);
            }
        };
    }
}
