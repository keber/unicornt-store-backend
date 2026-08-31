package com.unicornt.store.infrastructure.web.rest;

import com.unicornt.store.domain.service.AddressService;
import com.unicornt.store.infrastructure.persistence.entity.AddressEntity;
import com.unicornt.store.infrastructure.web.dto.AddressDtos.AddressCreateRequest;
import com.unicornt.store.infrastructure.web.dto.AddressDtos.AddressResponse;
import com.unicornt.store.infrastructure.web.error.ErrorResponse;
import com.unicornt.store.infrastructure.web.mapper.AddressMapper;
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
import java.util.List;

/**
 * Shipping addresses of the authenticated user. An address of another user is reported
 * as missing instead of forbidden.
 */
@RestController
@RequestMapping("/api/v1/addresses")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Addresses", description = "Shipping addresses of the authenticated user")
public class AddressRestController {

    private final AddressService addressService;

    public AddressRestController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    @Operation(summary = "List the addresses of the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Addresses of the caller"),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public List<AddressResponse> list(Authentication authentication) {
        return addressService.findByUser(authentication.getName()).stream()
                .map(AddressMapper::toResponse)
                .toList();
    }

    @PostMapping
    @Operation(summary = "Register a shipping address; the first address of a user becomes the default one")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Address created"),
            @ApiResponse(responseCode = "400", description = "Invalid payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AddressResponse> create(@Valid @RequestBody AddressCreateRequest body,
                                                  Authentication authentication) {
        AddressEntity created = addressService.create(authentication.getName(), AddressMapper.toEntity(body));
        return ResponseEntity.created(URI.create("/api/v1/addresses/" + created.getId()))
                .body(AddressMapper.toResponse(created));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete one address of the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Address deleted"),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "The address does not exist or is not the caller's",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void delete(@PathVariable Long id, Authentication authentication) {
        addressService.delete(authentication.getName(), id);
    }
}
