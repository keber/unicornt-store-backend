package com.unicornt.store.infrastructure.web.rest;

import com.unicornt.store.domain.service.ProductService;
import com.unicornt.store.infrastructure.persistence.entity.ProductEntity;
import com.unicornt.store.infrastructure.web.dto.ProductDtos.ProductCreateRequest;
import com.unicornt.store.infrastructure.web.dto.ProductDtos.ProductResponse;
import com.unicornt.store.infrastructure.web.dto.ProductDtos.ProductUpdateRequest;
import com.unicornt.store.infrastructure.web.error.ErrorResponse;
import com.unicornt.store.infrastructure.web.mapper.ProductMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/** Catalog product resource. Reads are public; writes require the administrator role. */
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Catalog product management")
public class ProductRestController {

    private final ProductService productService;

    public ProductRestController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "List products",
            description = "Returns a page of catalog products, optionally filtered by category and free text")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of products"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or filter parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Page<ProductResponse> list(
            @Parameter(description = "Category slug or category name", example = "hoodies")
            @RequestParam(required = false) String category,
            @Parameter(description = "Free text matched against name and description", example = "unicorn")
            @RequestParam(required = false) String q,
            @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return productService.search(category, q, pageable).map(ProductMapper::toResponse);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a product by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The requested product"),
            @ApiResponse(responseCode = "404", description = "Product does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ProductResponse getById(
            @Parameter(description = "Product identifier", example = "42") @PathVariable int id) {
        return ProductMapper.toResponse(productService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a product", description = "Administrator only")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created, its URI is in the Location header"),
            @ApiResponse(responseCode = "400", description = "Invalid payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Administrator role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Referenced category or product type does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductCreateRequest request) {
        ProductEntity created = productService.create(ProductMapper.toEntity(request));
        return ResponseEntity
                .created(URI.create("/api/v1/products/" + created.getId()))
                .body(ProductMapper.toResponse(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Replace a product", description = "Administrator only")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated product"),
            @ApiResponse(responseCode = "400", description = "Invalid payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Administrator role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product, category or product type does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ProductResponse update(
            @Parameter(description = "Product identifier", example = "42") @PathVariable int id,
            @Valid @RequestBody ProductUpdateRequest request) {
        return ProductMapper.toResponse(productService.update(id, ProductMapper.toEntity(request)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete a product", description = "Administrator only")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Product deleted, no body returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Administrator role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void delete(
            @Parameter(description = "Product identifier", example = "42") @PathVariable int id) {
        productService.delete(id);
    }
}
