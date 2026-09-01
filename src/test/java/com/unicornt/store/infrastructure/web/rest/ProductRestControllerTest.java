package com.unicornt.store.infrastructure.web.rest;

import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.domain.service.ProductService;
import com.unicornt.store.infrastructure.persistence.entity.ProductEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web slice for the product resource. The security filter chain is disabled here on
 * purpose: this test pins the HTTP contract, while authorization is covered by the
 * security task through the request matchers listed in the handoff note.
 */
@WebMvcTest(ProductRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductRestControllerTest {

    private static final String VALID_PAYLOAD = """
            {
              "name": "Unicorn hoodie",
              "description": "Cotton hoodie",
              "imageBase": "hoodie-unicorn",
              "price": 25990,
              "categoryId": 3,
              "productTypeId": 1,
              "active": true
            }
            """;

    private static final String INVALID_PAYLOAD = """
            {
              "name": "  ",
              "price": -5,
              "categoryId": 3,
              "productTypeId": 1
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    // SecurityConfig (T2) is picked up by @WebMvcTest's security auto-configuration and needs
    // these beans to construct its filter chain; mocked here so this slice stays narrow.
    @MockitoBean
    private com.unicornt.store.infrastructure.security.JwtService jwtService;

    @MockitoBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Test
    @DisplayName("GET /api/v1/products returns a page of products")
    void listReturnsPage() throws Exception {
        when(productService.search(eq("hoodies"), eq("unicorn"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleEntity()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/products").param("category", "hoodies").param("q", "unicorn"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(42))
                .andExpect(jsonPath("$.content[0].name").value("Unicorn hoodie"))
                .andExpect(jsonPath("$.content[0].categoryName").value("Hoodies"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} on a missing id returns 404 with RESOURCE_NOT_FOUND")
    void getByIdUnknownReturnsNotFound() throws Exception {
        when(productService.findById(999)).thenThrow(new ResourceNotFoundException("Product", 999));

        mockMvc.perform(get("/api/v1/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/v1/products/999"))
                .andExpect(jsonPath("$.message").value("Product not found: 999"));
    }

    @Test
    @DisplayName("POST /api/v1/products with an invalid payload returns 400 with populated errors[]")
    void createInvalidReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVALID_PAYLOAD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[?(@.field == 'name')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'price')]").exists());
    }

    @Test
    @DisplayName("POST /api/v1/products with a valid payload returns 201 and a Location header")
    void createValidReturnsCreated() throws Exception {
        when(productService.create(any(ProductEntity.class))).thenReturn(sampleEntity());

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PAYLOAD))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/products/42"))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.name").value("Unicorn hoodie"));
    }

    @Test
    @DisplayName("DELETE /api/v1/products/{id} returns 204 with an empty body")
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/products/42"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(productService).delete(42);
    }

    @Test
    @DisplayName("DELETE /api/v1/products/{id} on a missing id returns 404")
    void deleteUnknownReturnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Product", 999)).when(productService).delete(anyInt());

        mockMvc.perform(delete("/api/v1/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("The paginated payload keeps the documented envelope")
    void pageEnvelopeIsStable() throws Exception {
        when(productService.search(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleEntity()), PageRequest.of(0, 20), 1));

        String body = mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("\"content\"").contains("\"totalElements\"").contains("\"totalPages\"");
    }

    private ProductEntity sampleEntity() {
        ProductEntity entity = new ProductEntity();
        entity.setId(42);
        entity.setName("Unicorn hoodie");
        entity.setDescription("Cotton hoodie");
        entity.setImageBase("hoodie-unicorn");
        entity.setPrice(25990);
        entity.setCategoryId(3);
        entity.setCategoryName("Hoodies");
        entity.setProductTypeId(1);
        entity.setProductTypeName("Apparel");
        entity.setActive(true);
        return entity;
    }
}
