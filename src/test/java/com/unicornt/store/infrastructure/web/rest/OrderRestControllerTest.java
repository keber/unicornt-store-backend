package com.unicornt.store.infrastructure.web.rest;

import com.unicornt.store.domain.exception.OutOfStockException;
import com.unicornt.store.domain.exception.ResourceNotFoundException;
import com.unicornt.store.domain.service.CheckoutService;
import com.unicornt.store.infrastructure.persistence.entity.OrderEntity;
import com.unicornt.store.infrastructure.persistence.entity.OrderItemEntity;
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
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test for the order controller, wired standalone so it never depends on the
 * (still stub) SecurityConfig owned by the security task. Every request carries the
 * authenticated principal explicitly, the same {@code Authentication} the real filter
 * chain would attach to the request at runtime.
 */
class OrderRestControllerTest {

    private static final Authentication AUTH =
            new UsernamePasswordAuthenticationToken("buyer@unicornt.dev", "n/a", List.of());

    @Mock
    private CheckoutService checkoutService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(new OrderRestController(checkoutService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(new LocalValidatorFactoryBean())
                .build();
    }

    private static OrderEntity confirmedOrder() {
        OrderEntity order = new OrderEntity();
        order.setId(58L);
        order.setUserId(1L);
        order.setAddressId(7L);
        order.setShippingAddress("Av. Providencia 1234, Santiago, Region Metropolitana 7500000");
        order.setStatus(OrderEntity.OrderStatus.CONFIRMED);
        order.setCreatedAt(Instant.parse("2026-01-15T10:00:00Z"));
        order.setTotal(new BigDecimal("29980"));
        OrderItemEntity item = new OrderItemEntity(12, "Unicorn plush",
                new BigDecimal("14990"), 2, new BigDecimal("29980"));
        item.setId(101L);
        order.addItem(item);
        return order;
    }

    @Test
    void confirmReturns201WithLocationAndBody() throws Exception {
        when(checkoutService.confirm(eq("buyer@unicornt.dev"), eq(7L))).thenReturn(confirmedOrder());

        mockMvc.perform(post("/api/v1/orders").principal(AUTH).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":7}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/orders/58"))
                .andExpect(jsonPath("$.id").value(58))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.total").value(29980))
                .andExpect(jsonPath("$.items[0].productName").value("Unicorn plush"));
    }

    @Test
    void confirmReturns422WhenAProductIsOutOfStock() throws Exception {
        when(checkoutService.confirm(eq("buyer@unicornt.dev"), eq(7L)))
                .thenThrow(new OutOfStockException(12));

        mockMvc.perform(post("/api/v1/orders").principal(AUTH).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":7}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("Out of stock for product 12"));
    }

    @Test
    void confirmReturns400WhenTheAddressIsMissingFromThePayload() throws Exception {
        mockMvc.perform(post("/api/v1/orders").principal(AUTH).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("addressId"));
    }

    @Test
    void getReturns404WhenTheOrderBelongsToAnotherUser() throws Exception {
        when(checkoutService.findOrder(eq("buyer@unicornt.dev"), eq(99L)))
                .thenThrow(new ResourceNotFoundException("Order", 99L));

        mockMvc.perform(get("/api/v1/orders/99").principal(AUTH))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Order not found: 99"));
    }

    @Test
    void listReturnsTheOrdersOfTheCaller() throws Exception {
        when(checkoutService.findOrders(eq("buyer@unicornt.dev"))).thenReturn(List.of(confirmedOrder()));

        mockMvc.perform(get("/api/v1/orders").principal(AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(58))
                .andExpect(jsonPath("$[0].items[0].quantity").value(2));
    }
}
