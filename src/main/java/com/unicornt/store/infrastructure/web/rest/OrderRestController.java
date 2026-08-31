package com.unicornt.store.infrastructure.web.rest;

import com.unicornt.store.domain.service.CheckoutService;
import com.unicornt.store.infrastructure.persistence.entity.OrderEntity;
import com.unicornt.store.infrastructure.web.dto.OrderDtos.OrderCreateRequest;
import com.unicornt.store.infrastructure.web.dto.OrderDtos.OrderResponse;
import com.unicornt.store.infrastructure.web.error.ErrorResponse;
import com.unicornt.store.infrastructure.web.mapper.OrderMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * Orders of the authenticated user. An order of another user is reported as missing,
 * so the API never leaks the existence of somebody else's order.
 */
@RestController
@RequestMapping("/api/v1/orders")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Orders", description = "Confirmation and history of the orders of the authenticated user")
public class OrderRestController {

    private final CheckoutService checkoutService;

    public OrderRestController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping
    @Operation(summary = "Confirm the current cart as a new order")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order confirmed"),
            @ApiResponse(responseCode = "400", description = "Invalid payload or empty cart",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "The address does not belong to the caller",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "A product of the cart is out of stock",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OrderResponse> confirm(@Valid @RequestBody OrderCreateRequest body,
                                                 Authentication authentication) {
        OrderEntity order = checkoutService.confirm(authentication.getName(), body.addressId());
        return ResponseEntity.created(URI.create("/api/v1/orders/" + order.getId()))
                .body(OrderMapper.toResponse(order));
    }

    @GetMapping
    @Operation(summary = "List the orders of the authenticated user, most recent first")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orders of the caller"),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public List<OrderResponse> list(Authentication authentication) {
        return checkoutService.findOrders(authentication.getName()).stream()
                .map(OrderMapper::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one order of the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The requested order"),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "The order does not exist or is not the caller's",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public OrderResponse get(@PathVariable Long id, Authentication authentication) {
        return OrderMapper.toResponse(checkoutService.findOrder(authentication.getName(), id));
    }
}
