package com.unicornt.store.infrastructure.web.rest;

import com.unicornt.store.application.usecase.ordering.GetOrderUseCase;
import com.unicornt.store.application.usecase.ordering.ListOrdersUseCase;
import com.unicornt.store.application.usecase.ordering.OrderConfirmation;
import com.unicornt.store.application.usecase.ordering.PlaceOrderUseCase;
import com.unicornt.store.domain.exception.OutOfStockException;
import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.domain.model.Order;
import com.unicornt.store.domain.model.OrderItem;
import com.unicornt.store.domain.model.ShippingAddress;
import com.unicornt.store.domain.valueobject.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.security.Principal;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web slice for the order resource. The security filter chain is disabled; the
 * principal is supplied explicitly. Each method mocks the use case it calls.
 */
@WebMvcTest(OrderRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderRestControllerTest {

    private static final Principal ADA = () -> "ada@example.com";
    private static final String VALID_BODY = """
            { "shippingAddress": { "street": "Av. Providencia 1234",
                                   "city": "Santiago", "region": "RM", "zipCode": "7500000" } }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlaceOrderUseCase placeOrder;
    @MockitoBean
    private GetOrderUseCase getOrder;
    @MockitoBean
    private ListOrdersUseCase listOrders;
    @MockitoBean
    private com.unicornt.store.infrastructure.security.JwtService jwtService;
    @MockitoBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    private static Order sampleOrder() {
        return Order.place("ada@example.com",
                new ShippingAddress("Av. Providencia 1234", "Santiago", "RM", "7500000"),
                List.of(new OrderItem(2L, "Rainbow Mug", Money.ofClp(7990), 3)));
    }

    @Test
    @DisplayName("POST /api/v1/orders confirms the cart and returns 201 with a Location header")
    void placeReturnsCreated() throws Exception {
        when(placeOrder.execute(eq("ada@example.com"), any(ShippingAddress.class)))
                .thenReturn(new OrderConfirmation(58L, "CONFIRMED", 23970));

        mockMvc.perform(post("/api/v1/orders").principal(ADA)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/orders/58"))
                .andExpect(jsonPath("$.id").value(58))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.total").value(23970));
    }

    @Test
    @DisplayName("POST /api/v1/orders with an incomplete address returns 400 with errors[]")
    void placeInvalidAddressReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/orders").principal(ADA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"shippingAddress\": { \"street\": \"  \", \"city\": \"Santiago\", \"region\": \"RM\" } }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/v1/orders on an empty cart returns 400")
    void placeEmptyCartReturnsBadRequest() throws Exception {
        when(placeOrder.execute(any(), any())).thenThrow(new IllegalArgumentException("The cart is empty"));

        mockMvc.perform(post("/api/v1/orders").principal(ADA)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The cart is empty"));
    }

    @Test
    @DisplayName("POST /api/v1/orders when a product is out of stock returns 422 BUSINESS_RULE_VIOLATION")
    void placeOutOfStockReturnsUnprocessable() throws Exception {
        when(placeOrder.execute(any(), any())).thenThrow(new OutOfStockException(2));

        mockMvc.perform(post("/api/v1/orders").principal(ADA)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{id} returns the full order")
    void getReturnsOrder() throws Exception {
        when(getOrder.execute(58L, "ada@example.com")).thenReturn(sampleOrder());

        mockMvc.perform(get("/api/v1/orders/58").principal(ADA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.total").value(23970))
                .andExpect(jsonPath("$.shippingAddress.city").value("Santiago"))
                .andExpect(jsonPath("$.items[0].productName").value("Rainbow Mug"))
                .andExpect(jsonPath("$.items[0].subtotal").value(23970));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{id} for a missing or foreign order returns 404")
    void getUnknownReturnsNotFound() throws Exception {
        when(getOrder.execute(999L, "ada@example.com"))
                .thenThrow(new ResourceNotFoundException("Order", 999));

        mockMvc.perform(get("/api/v1/orders/999").principal(ADA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/v1/orders lists the caller's orders")
    void listReturnsOrders() throws Exception {
        when(listOrders.execute("ada@example.com")).thenReturn(List.of(sampleOrder()));

        mockMvc.perform(get("/api/v1/orders").principal(ADA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].total").value(23970));
    }
}
