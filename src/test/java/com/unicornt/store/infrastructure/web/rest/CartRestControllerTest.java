package com.unicornt.store.infrastructure.web.rest;

import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.domain.service.CartService;
import com.unicornt.store.domain.service.CartService.CartLine;
import com.unicornt.store.domain.service.CartService.CartView;
import com.unicornt.store.infrastructure.web.error.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test for the cart controller, wired standalone so it never depends on the
 * (still stub) SecurityConfig owned by the security task. Every request carries the
 * authenticated principal explicitly, the same {@code Authentication} the real filter
 * chain would attach to the request at runtime.
 */
class CartRestControllerTest {

    private static final Authentication AUTH =
            new UsernamePasswordAuthenticationToken("buyer@unicornt.dev", "n/a", List.of());

    private static final CartLine LINE = new CartLine(45L, 12, "Unicorn plush", "unicorn-plush",
            new BigDecimal("14990"), 2, new BigDecimal("29980"));

    @Mock
    private CartService cartService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(new CartRestController(cartService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(new LocalValidatorFactoryBean())
                .build();
    }

    @Test
    void getCartReturnsThePricedCartOfTheCaller() throws Exception {
        when(cartService.getCart(eq("buyer@unicornt.dev")))
                .thenReturn(new CartView(List.of(LINE), 2, new BigDecimal("29980")));

        mockMvc.perform(get("/api/v1/cart").principal(AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").value(2))
                .andExpect(jsonPath("$.total").value(29980))
                .andExpect(jsonPath("$.items[0].id").value(45))
                .andExpect(jsonPath("$.items[0].subtotal").value(29980));
    }

    @Test
    void addItemReturns201WithLocation() throws Exception {
        when(cartService.addItem(eq("buyer@unicornt.dev"), eq(12), eq(2))).thenReturn(LINE);

        mockMvc.perform(post("/api/v1/cart/items").principal(AUTH).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":12,\"qty\":2}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/cart/items/45"))
                .andExpect(jsonPath("$.productId").value(12));
    }

    @Test
    void addItemReturns400WhenTheQuantityIsZero() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items").principal(AUTH).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":12,\"qty\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("qty"))
                .andExpect(jsonPath("$.errors[0].message").value("qty must be greater than 0"));

        verify(cartService, never()).addItem(anyString(), anyInt(), anyInt());
    }

    @Test
    void removeItemReturns204WithAnEmptyBody() throws Exception {
        mockMvc.perform(delete("/api/v1/cart/items/45").principal(AUTH).with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(cartService).removeItem("buyer@unicornt.dev", 45L);
    }

    @Test
    void removeItemReturns404WhenTheLineBelongsToAnotherUser() throws Exception {
        doThrow(new ResourceNotFoundException("Cart item", 99L))
                .when(cartService).removeItem("buyer@unicornt.dev", 99L);

        mockMvc.perform(delete("/api/v1/cart/items/99").principal(AUTH).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
