package com.unicornt.store.infrastructure.web.rest;

import com.unicornt.store.application.usecase.catalog.CreateCategoryUseCase;
import com.unicornt.store.application.usecase.catalog.ListCategoriesUseCase;
import com.unicornt.store.domain.exception.DuplicateResourceException;
import com.unicornt.store.domain.model.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Web slice for the category resource; the filter chain is disabled as in the product slice. */
@WebMvcTest(CategoryRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListCategoriesUseCase listCategories;
    @MockitoBean
    private CreateCategoryUseCase createCategory;

    // SecurityConfig is picked up by @WebMvcTest security auto-configuration and needs
    // these beans to construct its filter chain; mocked here so this slice stays narrow.
    @MockitoBean
    private com.unicornt.store.infrastructure.security.JwtService jwtService;
    @MockitoBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Test
    @DisplayName("GET /api/v1/categories returns every category")
    void listReturnsCategories() throws Exception {
        when(listCategories.execute()).thenReturn(List.of(new Category(3L, "Hoodies", "hoodies")));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].name").value("Hoodies"))
                .andExpect(jsonPath("$[0].slug").value("hoodies"));
    }

    @Test
    @DisplayName("POST /api/v1/categories returns 201 and a Location header")
    void createReturnsCreated() throws Exception {
        when(createCategory.execute(eq("Hoodies"), any()))
                .thenReturn(new Category(3L, "Hoodies", "hoodies"));

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Hoodies\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/categories/3"))
                .andExpect(jsonPath("$.slug").value("hoodies"));
    }

    @Test
    @DisplayName("POST /api/v1/categories with a blank name returns 400 with populated errors[]")
    void createBlankNameReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    @DisplayName("POST /api/v1/categories with a taken slug returns 409")
    void createDuplicateReturnsConflict() throws Exception {
        when(createCategory.execute(any(), any()))
                .thenThrow(new DuplicateResourceException("Category", "slug", "hoodies"));

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Hoodies\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));
    }
}
