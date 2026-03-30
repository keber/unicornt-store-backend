package com.unicornt.store.controller;

import com.unicornt.store.model.Role;
import com.unicornt.store.repository.RoleRepository;
import com.unicornt.store.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class SecurityIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        if (roleRepository.findByName("ROLE_CLIENT").isEmpty()) {
            roleRepository.save(new Role("ROLE_CLIENT"));
        }
        if (roleRepository.findByName("ROLE_ADMIN").isEmpty()) {
            roleRepository.save(new Role("ROLE_ADMIN"));
        }
    }

    // ── Acceso público ──────────────────────────────────────────

    @Test
    void loginPage_debeSerAccesibleSinAutenticacion() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    void registerPage_debeSerAccesibleSinAutenticacion() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk());
    }

    @Test
    void rutaProtegida_debeRedirigirALoginSinAutenticacion() throws Exception {
        mockMvc.perform(get("/catalog"))
                .andExpect(status().is3xxRedirection());
    }

    // ── Acceso con rol CLIENT ───────────────────────────────────

    @Test
    void catalog_debeSerAccesibleParaClient() throws Exception {
        mockMvc.perform(get("/catalog")
                        .with(user("cliente@test.com").roles("CLIENT")))
                .andExpect(status().isOk());
    }

    @Test
    void adminProducts_debeSerDenegadoParaClient() throws Exception {
        mockMvc.perform(get("/admin/products")
                        .with(user("cliente@test.com").roles("CLIENT")))
                .andExpect(status().isForbidden());
    }

    // ── Acceso con rol ADMIN ────────────────────────────────────

    @Test
    void adminProducts_debeSerAccesibleParaAdmin() throws Exception {
        mockMvc.perform(get("/admin/products")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void catalog_debeSerAccesibleParaAdmin() throws Exception {
        mockMvc.perform(get("/catalog")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    // ── Registro ────────────────────────────────────────────────

    @Test
    void register_conDatosValidos_debeRedirigirALogin() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("firstName", "Test")
                        .param("lastName", "User")
                        .param("email", "nuevo@test.com")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));
    }

    @Test
    void register_conCamposVacios_debeMostrarError() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("firstName", "")
                        .param("lastName", "")
                        .param("email", "")
                        .param("password", ""))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void register_conPasswordCorta_debeMostrarError() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("firstName", "Test")
                        .param("lastName", "User")
                        .param("email", "corta@test.com")
                        .param("password", "abc"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void register_conEmailDuplicado_debeMostrarError() throws Exception {
        // Primer registro
        mockMvc.perform(post("/register")
                .with(csrf())
                .param("firstName", "A")
                .param("lastName", "B")
                .param("email", "duplicado@test.com")
                .param("password", "password123"));

        // Segundo registro con mismo email
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("firstName", "C")
                        .param("lastName", "D")
                        .param("email", "duplicado@test.com")
                        .param("password", "password456"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"));
    }
}
