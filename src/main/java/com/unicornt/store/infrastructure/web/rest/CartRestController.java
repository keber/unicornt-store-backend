package com.unicornt.store.infrastructure.web.rest;

import com.unicornt.store.application.usecase.cart.AddCartItemUseCase;
import com.unicornt.store.application.usecase.cart.GetCartUseCase;
import com.unicornt.store.application.usecase.cart.MergeCartUseCase;
import com.unicornt.store.application.usecase.cart.RemoveCartItemUseCase;
import com.unicornt.store.application.usecase.cart.UpdateCartItemUseCase;
import com.unicornt.store.infrastructure.web.dto.CartDtos.AddCartItemRequest;
import com.unicornt.store.infrastructure.web.dto.CartDtos.CartResponse;
import com.unicornt.store.infrastructure.web.dto.CartDtos.MergeCartRequest;
import com.unicornt.store.infrastructure.web.dto.CartDtos.UpdateCartItemRequest;
import com.unicornt.store.infrastructure.web.error.ErrorResponse;
import com.unicornt.store.infrastructure.web.mapper.CartRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Cart of the authenticated user. Thin: every method reads the principal from the
 * security context, calls one use case and maps the result. The identity never
 * comes from the request, so no caller can reach another user's cart. Every
 * mutating endpoint answers with the whole updated cart.
 */
@RestController
@RequestMapping("/api/v1/cart")
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cart", description = "Shopping cart of the authenticated user")
public class CartRestController {

    private final GetCartUseCase getCart;
    private final AddCartItemUseCase addCartItem;
    private final UpdateCartItemUseCase updateCartItem;
    private final RemoveCartItemUseCase removeCartItem;
    private final MergeCartUseCase mergeCart;

    public CartRestController(GetCartUseCase getCart,
                             AddCartItemUseCase addCartItem,
                             UpdateCartItemUseCase updateCartItem,
                             RemoveCartItemUseCase removeCartItem,
                             MergeCartUseCase mergeCart) {
        this.getCart = getCart;
        this.addCartItem = addCartItem;
        this.updateCartItem = updateCartItem;
        this.removeCartItem = removeCartItem;
        this.mergeCart = mergeCart;
    }

    @GetMapping
    @Operation(summary = "Get the cart of the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart of the caller"),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public CartResponse getCart(Authentication authentication) {
        return CartRestMapper.toResponse(getCart.execute(authentication.getName()));
    }

    @PostMapping("/items")
    @Operation(summary = "Add a product to the cart, or increase the quantity of an existing line")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quantity increased on an existing line"),
            @ApiResponse(responseCode = "201", description = "New line created"),
            @ApiResponse(responseCode = "400", description = "Invalid payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "The product does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CartResponse> addItem(@Valid @RequestBody AddCartItemRequest body,
                                                Authentication authentication) {
        AddCartItemUseCase.Result result = addCartItem.execute(
                authentication.getName(), body.productId(), body.quantity());
        CartResponse response = CartRestMapper.toResponse(result.cart());
        if (result.created()) {
            return ResponseEntity
                    .created(URI.create("/api/v1/cart/items/" + body.productId()))
                    .body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PutMapping("/items/{productId}")
    @Operation(summary = "Set the quantity of one line of the cart; a quantity of 0 removes it")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated cart"),
            @ApiResponse(responseCode = "400", description = "Invalid payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "The product is not a line of the cart",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public CartResponse updateItem(
            @Parameter(description = "Product identifier", example = "12") @PathVariable long productId,
            @Valid @RequestBody UpdateCartItemRequest body,
            Authentication authentication) {
        return CartRestMapper.toResponse(
                updateCartItem.execute(authentication.getName(), productId, body.quantity()));
    }

    @DeleteMapping("/items/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove one line from the cart")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Line removed"),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "The product is not a line of the cart",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void removeItem(
            @Parameter(description = "Product identifier", example = "12") @PathVariable long productId,
            Authentication authentication) {
        removeCartItem.execute(authentication.getName(), productId);
    }

    @PostMapping("/merge")
    @Operation(summary = "Merge a local (anonymous) cart into the authenticated user's cart",
            description = "For each line the new quantity is server + local, clamped to the product's stock")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Merged cart"),
            @ApiResponse(responseCode = "400", description = "Invalid payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public CartResponse merge(@Valid @RequestBody MergeCartRequest body, Authentication authentication) {
        return CartRestMapper.toResponse(
                mergeCart.execute(authentication.getName(), CartRestMapper.toIncomingItems(body)));
    }
}
