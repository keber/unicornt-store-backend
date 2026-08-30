package com.unicornt.store;

import com.unicornt.store.infrastructure.persistence.entity.RoleEntity;
import com.unicornt.store.infrastructure.persistence.entity.UserEntity;
import com.unicornt.store.infrastructure.persistence.repository.RoleRepository;
import com.unicornt.store.infrastructure.persistence.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

/**
 * Entry point of the Spring Boot application.
 * Extends SpringBootServletInitializer to allow deployment as a WAR
 * on an external Tomcat (10.1+).
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
    @ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
    CommandLineRunner initData(RoleRepository roleRepository,
                               UserRepository userRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            // Create roles if they do not exist
            RoleEntity adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> roleRepository.save(new RoleEntity("ROLE_ADMIN")));
            RoleEntity clientRole = roleRepository.findByName("ROLE_CLIENT")
                    .orElseGet(() -> roleRepository.save(new RoleEntity("ROLE_CLIENT")));

            // Create the admin user if it does not exist
            if (userRepository.findByEmail("admin@unicornt.cl").isEmpty()) {
                UserEntity admin = new UserEntity();
                admin.setFirstName("Admin");
                admin.setLastName("Store");
                admin.setEmail("admin@unicornt.cl");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRoles(Set.of(adminRole));
                userRepository.save(admin);
            }

            // Create the demo client user if it does not exist
            if (userRepository.findByEmail("cliente@unicornt.cl").isEmpty()) {
                UserEntity client = new UserEntity();
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
