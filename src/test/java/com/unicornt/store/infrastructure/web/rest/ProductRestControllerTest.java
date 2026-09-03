package com.unicornt.store.infrastructure.web.rest;

import com.unicornt.store.application.usecase.catalog.CreateProductUseCase;
import com.unicornt.store.application.usecase.catalog.DeleteProductUseCase;
import com.unicornt.store.application.usecase.catalog.GetProductUseCase;
import com.unicornt.store.application.usecase.catalog.ListProductsUseCase;
import com.unicornt.store.application.usecase.catalog.ProductCommand;
import com.unicornt.store.application.usecase.catalog.UpdateProductUseCase;
import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.domain.model.Product;
import com.unicornt.store.domain.repository.PageResult;
import com.unicornt.store.domain.valueobject.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
 * purpose: this test pins the HTTP contract; authorization is covered by the
 * identity slice. Each method mocks the use case it calls.
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
              "stock": 40,
              "active": true
            }
            """;

    private static final String INVALID_PAYLOAD = """
            {
              "name": "  ",
              "price": -5,
              "categoryId": 3,
              "productTypeId": 1,
              "stock": 3
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListProductsUseCase listProducts;
    @MockitoBean
    private GetProductUseCase getProduct;
    @MockitoBean
    private CreateProductUseCase createProduct;
    @MockitoBean
    private UpdateProductUseCase updateProduct;
    @MockitoBean
    private DeleteProductUseCase deleteProduct;

    // SecurityConfig is picked up by @WebMvcTest security auto-configuration and needs
    // these beans to construct its filter chain; mocked here so this slice stays narrow.
    @MockitoBean
    private com.unicornt.store.infrastructure.security.JwtService jwtService;
    @MockitoBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Test
    @DisplayName("GET /api/v1/products returns a page of products")
    void listReturnsPage() throws Exception {
        when(listProducts.execute(eq("unicorns"), eq("unicorn"), eq(0), eq(20)))
                .thenReturn(new PageResult<>(List.of(sampleProduct()), 0, 20, 1));

        mockMvc.perform(get("/api/v1/products").param("category", "unicorns").param("q", "unicorn"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(42))
                .andExpect(jsonPath("$.content[0].name").value("Unicorn hoodie"))
                .andExpect(jsonPath("$.content[0].categoryName").value("Unicorns"))
                .andExpect(jsonPath("$.content[0].stock").value(40))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} on a missing id returns 404 with RESOURCE_NOT_FOUND")
    void getByIdUnknownReturnsNotFound() throws Exception {
        when(getProduct.execute(999L)).thenThrow(new ResourceNotFoundException("Product", 999));

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
        when(createProduct.execute(any(ProductCommand.class))).thenReturn(sampleProduct());

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

        verify(deleteProduct).execute(42L);
    }

    @Test
    @DisplayName("DELETE /api/v1/products/{id} on a missing id returns 404")
    void deleteUnknownReturnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Product", 999)).when(deleteProduct).execute(anyLong());

        mockMvc.perform(delete("/api/v1/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("The paginated payload keeps the documented envelope")
    void pageEnvelopeIsStable() throws Exception {
        when(listProducts.execute(any(), any(), eq(0), eq(20)))
                .thenReturn(new PageResult<>(List.of(sampleProduct()), 0, 20, 1));

        String body = mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("\"content\"").contains("\"totalElements\"").contains("\"totalPages\"");
    }

    private Product sampleProduct() {
        return new Product(42L, "Unicorn hoodie", "Cotton hoodie", "hoodie-unicorn",
                Money.ofClp(25990), 3L, "Unicorns", 1L, "T-shirt", 40, true);
    }
}
