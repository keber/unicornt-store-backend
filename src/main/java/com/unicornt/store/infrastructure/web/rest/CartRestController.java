package com.unicornt.store.infrastructure.web.rest;

import com.unicornt.store.domain.service.CartService;
import com.unicornt.store.infrastructure.web.dto.CartDtos.CartItemQuantityRequest;
import com.unicornt.store.infrastructure.web.dto.CartDtos.CartItemRequest;
import com.unicornt.store.infrastructure.web.dto.CartDtos.CartItemResponse;
import com.unicornt.store.infrastructure.web.dto.CartDtos.CartResponse;
import com.unicornt.store.infrastructure.web.error.ErrorResponse;
import com.unicornt.store.infrastructure.web.mapper.CartMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * Cart of the authenticated user. The identity always comes from the security context,
 * never from the request, so no caller can reach another user's cart.
 */
@RestController
@RequestMapping("/api/v1/cart")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Cart", description = "Shopping cart of the authenticated user")
public class CartRestController {

    private final CartService cartService;

    public CartRestController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    @Operation(summary = "Get the cart of the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart of the caller"),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public CartResponse getCart(Authentication authentication) {
        return CartMapper.toResponse(cartService.getCart(authentication.getName()));
    }

    @PostMapping("/items")
    @Operation(summary = "Add a product to the cart, or increase the quantity of an existing line")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Line created or increased"),
            @ApiResponse(responseCode = "400", description = "Invalid payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "The product does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CartItemResponse> addItem(@Valid @RequestBody CartItemRequest body,
                                                    Authentication authentication) {
        CartItemResponse item = CartMapper.toResponse(
                cartService.addItem(authentication.getName(), body.productId(), body.qty()));
        return ResponseEntity.created(URI.create("/api/v1/cart/items/" + item.id())).body(item);
    }

    @PatchMapping("/items/{id}")
    @Operation(summary = "Replace the quantity of one line of the cart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated line"),
            @ApiResponse(responseCode = "400", description = "Invalid payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "The line does not belong to the caller",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public CartItemResponse updateItem(@PathVariable Long id,
                                       @Valid @RequestBody CartItemQuantityRequest body,
                                       Authentication authentication) {
        return CartMapper.toResponse(
                cartService.updateItemQuantity(authentication.getName(), id, body.qty()));
    }

    @DeleteMapping("/items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove one line from the cart")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Line removed"),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "The line does not belong to the caller",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void removeItem(@PathVariable Long id, Authentication authentication) {
        cartService.removeItem(authentication.getName(), id);
    }
}
