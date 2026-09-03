package com.unicornt.store.infrastructure.web.rest;

import com.unicornt.store.application.usecase.ordering.GetOrderUseCase;
import com.unicornt.store.application.usecase.ordering.ListOrdersUseCase;
import com.unicornt.store.application.usecase.ordering.OrderConfirmation;
import com.unicornt.store.application.usecase.ordering.PlaceOrderUseCase;
import com.unicornt.store.infrastructure.web.dto.OrderDtos.OrderConfirmationResponse;
import com.unicornt.store.infrastructure.web.dto.OrderDtos.OrderResponse;
import com.unicornt.store.infrastructure.web.dto.OrderDtos.PlaceOrderRequest;
import com.unicornt.store.infrastructure.web.error.ErrorResponse;
import com.unicornt.store.infrastructure.web.mapper.OrderRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.security.Principal;
import java.util.List;

/**
 * Orders of the authenticated user. Thin: read the principal, call one use case,
 * map the result. An order of another user reads as missing.
 */
@RestController
@RequestMapping("/api/v1/orders")
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Orders", description = "Confirmation and history of the authenticated user's orders")
public class OrderRestController {

    private final PlaceOrderUseCase placeOrder;
    private final GetOrderUseCase getOrder;
    private final ListOrdersUseCase listOrders;

    public OrderRestController(PlaceOrderUseCase placeOrder, GetOrderUseCase getOrder,
                              ListOrdersUseCase listOrders) {
        this.placeOrder = placeOrder;
        this.getOrder = getOrder;
        this.listOrders = listOrders;
    }

    @PostMapping
    @Operation(summary = "Confirm the current cart as a new order",
            description = "Items come from the server cart; the address is in the body. Payment is simulated.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order confirmed"),
            @ApiResponse(responseCode = "400", description = "Invalid payload or empty cart",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "A product of the cart is out of stock",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OrderConfirmationResponse> place(@Valid @RequestBody PlaceOrderRequest body,
                                                           Principal principal) {
        OrderConfirmation confirmation = placeOrder.execute(
                principal.getName(), OrderRestMapper.toDomain(body.shippingAddress()));
        return ResponseEntity
                .created(URI.create("/api/v1/orders/" + confirmation.id()))
                .body(OrderRestMapper.toResponse(confirmation));
    }

    @GetMapping
    @Operation(summary = "List the authenticated user's orders, most recent first")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orders of the caller"),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public List<OrderResponse> list(Principal principal) {
        return listOrders.execute(principal.getName()).stream()
                .map(OrderRestMapper::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one of the authenticated user's orders")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The requested order"),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "The order does not exist or is not the caller's",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public OrderResponse get(
            @Parameter(description = "Order identifier", example = "58") @PathVariable long id,
            Principal principal) {
        return OrderRestMapper.toResponse(getOrder.execute(id, principal.getName()));
    }
}
