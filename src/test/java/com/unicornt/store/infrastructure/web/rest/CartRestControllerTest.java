package com.unicornt.store.infrastructure.web.rest;

import com.unicornt.store.application.usecase.cart.AddCartItemUseCase;
import com.unicornt.store.application.usecase.cart.GetCartUseCase;
import com.unicornt.store.application.usecase.cart.MergeCartUseCase;
import com.unicornt.store.application.usecase.cart.MergeCartUseCase.IncomingItem;
import com.unicornt.store.application.usecase.cart.PricedCart;
import com.unicornt.store.application.usecase.cart.RemoveCartItemUseCase;
import com.unicornt.store.application.usecase.cart.UpdateCartItemUseCase;
import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.domain.valueobject.Money;
import com.unicornt.store.infrastructure.web.error.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test for the cart controller, wired standalone so it never depends on the
 * SecurityConfig owned by the identity slice. Every request carries the
 * authenticated principal explicitly, the same {@code Authentication} the real
 * filter chain would attach at runtime. This test pins the HTTP contract; each
 * method mocks the use case it calls.
 */
@DisplayName("CartRestController")
class CartRestControllerTest {

    private static final Authentication AUTH =
            new UsernamePasswordAuthenticationToken("buyer@unicornt.dev", "n/a", List.of());

    private static PricedCart pricedCart() {
        return new PricedCart(List.of(new PricedCart.Line(
                12L, "Unicorn plush", "unicorn-plush", Money.ofClp(14990), 2, Money.ofClp(29980))),
                2, Money.ofClp(29980));
    }

    @Mock
    private GetCartUseCase getCart;
    @Mock
    private AddCartItemUseCase addCartItem;
    @Mock
    private UpdateCartItemUseCase updateCartItem;
    @Mock
    private RemoveCartItemUseCase removeCartItem;
    @Mock
    private MergeCartUseCase mergeCart;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CartRestController(getCart, addCartItem, updateCartItem,
                        removeCartItem, mergeCart))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(new LocalValidatorFactoryBean())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/cart returns the priced cart of the caller")
    void getCartReturnsPricedCart() throws Exception {
        when(getCart.execute("buyer@unicornt.dev")).thenReturn(pricedCart());

        mockMvc.perform(get("/api/v1/cart").principal(AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").value(2))
                .andExpect(jsonPath("$.total").value(29980))
                .andExpect(jsonPath("$.items[0].productId").value(12))
                .andExpect(jsonPath("$.items[0].unitPrice").value(14990))
                .andExpect(jsonPath("$.items[0].subtotal").value(29980));
    }

    @Test
    @DisplayName("POST /api/v1/cart/items creating a new line returns 201 with a Location header")
    void addItemCreatedReturns201() throws Exception {
        when(addCartItem.execute("buyer@unicornt.dev", 12L, 2))
                .thenReturn(new AddCartItemUseCase.Result(pricedCart(), true));

        mockMvc.perform(post("/api/v1/cart/items").principal(AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":12,\"quantity\":2}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/cart/items/12"))
                .andExpect(jsonPath("$.items[0].productId").value(12));
    }

    @Test
    @DisplayName("POST /api/v1/cart/items increasing an existing line returns 200")
    void addItemMergedReturns200() throws Exception {
        when(addCartItem.execute("buyer@unicornt.dev", 12L, 2))
                .thenReturn(new AddCartItemUseCase.Result(pricedCart(), false));

        mockMvc.perform(post("/api/v1/cart/items").principal(AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":12,\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"));
    }

    @Test
    @DisplayName("POST /api/v1/cart/items with quantity 0 returns 400 with a populated errors[]")
    void addItemZeroQuantityReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items").principal(AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":12,\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("quantity"));

        verify(addCartItem, never()).execute(anyString(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("POST /api/v1/cart/items for a missing product returns 404")
    void addItemMissingProductReturns404() throws Exception {
        when(addCartItem.execute("buyer@unicornt.dev", 999L, 1))
                .thenThrow(new ResourceNotFoundException("Product", 999));

        mockMvc.perform(post("/api/v1/cart/items").principal(AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":999,\"quantity\":1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("PUT /api/v1/cart/items/{productId} returns 200 with the updated cart")
    void updateItemReturns200() throws Exception {
        when(updateCartItem.execute("buyer@unicornt.dev", 12L, 3)).thenReturn(pricedCart());

        mockMvc.perform(put("/api/v1/cart/items/12").principal(AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").value(2));
    }

    @Test
    @DisplayName("PUT /api/v1/cart/items/{productId} with quantity 0 is accepted (removes the line)")
    void updateItemZeroQuantityRemoves() throws Exception {
        when(updateCartItem.execute("buyer@unicornt.dev", 12L, 0))
                .thenReturn(new PricedCart(List.of(), 0, Money.ofClp(0)));

        mockMvc.perform(put("/api/v1/cart/items/12").principal(AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    @DisplayName("PUT /api/v1/cart/items/{productId} with a negative quantity returns 400")
    void updateItemNegativeQuantityReturns400() throws Exception {
        mockMvc.perform(put("/api/v1/cart/items/12").principal(AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(updateCartItem, never()).execute(anyString(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("PUT /api/v1/cart/items/{productId} for a line not in the cart returns 404")
    void updateItemUnknownLineReturns404() throws Exception {
        when(updateCartItem.execute("buyer@unicornt.dev", 99L, 3))
                .thenThrow(new ResourceNotFoundException("Cart item", 99));

        mockMvc.perform(put("/api/v1/cart/items/99").principal(AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("DELETE /api/v1/cart/items/{productId} returns 204 with an empty body")
    void removeItemReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/cart/items/12").principal(AUTH))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(removeCartItem).execute("buyer@unicornt.dev", 12L);
    }

    @Test
    @DisplayName("DELETE /api/v1/cart/items/{productId} for a line not in the cart returns 404")
    void removeItemUnknownLineReturns404() throws Exception {
        doThrow(new ResourceNotFoundException("Cart item", 99))
                .when(removeCartItem).execute("buyer@unicornt.dev", 99L);

        mockMvc.perform(delete("/api/v1/cart/items/99").principal(AUTH))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /api/v1/cart/merge folds the local items in and returns the merged cart")
    void mergeReturnsMergedCart() throws Exception {
        when(mergeCart.execute(eq("buyer@unicornt.dev"), any())).thenReturn(pricedCart());

        mockMvc.perform(post("/api/v1/cart/merge").principal(AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":12,\"quantity\":1},{\"productId\":20,\"quantity\":2}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productId").value(12));

        verify(mergeCart).execute("buyer@unicornt.dev",
                List.of(new IncomingItem(12, 1), new IncomingItem(20, 2)));
    }

    @Test
    @DisplayName("POST /api/v1/cart/merge with no items returns 400")
    void mergeEmptyItemsReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/cart/merge").principal(AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(mergeCart, never()).execute(anyString(), any());
    }
}
