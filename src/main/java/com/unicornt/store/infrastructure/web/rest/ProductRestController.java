package com.unicornt.store.infrastructure.web.rest;

import com.unicornt.store.application.usecase.catalog.CreateProductUseCase;
import com.unicornt.store.application.usecase.catalog.DeleteProductUseCase;
import com.unicornt.store.application.usecase.catalog.GetProductUseCase;
import com.unicornt.store.application.usecase.catalog.ListProductsUseCase;
import com.unicornt.store.application.usecase.catalog.UpdateProductUseCase;
import com.unicornt.store.domain.model.Product;
import com.unicornt.store.infrastructure.web.dto.ProductDtos.ProductCreateRequest;
import com.unicornt.store.infrastructure.web.dto.ProductDtos.ProductPageResponse;
import com.unicornt.store.infrastructure.web.dto.ProductDtos.ProductResponse;
import com.unicornt.store.infrastructure.web.dto.ProductDtos.ProductUpdateRequest;
import com.unicornt.store.infrastructure.web.error.ErrorResponse;
import com.unicornt.store.infrastructure.web.mapper.ProductRestMapper;
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

/**
 * Catalog product resource. Thin: every method reads the request, calls one use case
 * and maps the result. Reads are public; writes require the administrator role.
 */
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Catalog product management")
public class ProductRestController {

    private final ListProductsUseCase listProducts;
    private final GetProductUseCase getProduct;
    private final CreateProductUseCase createProduct;
    private final UpdateProductUseCase updateProduct;
    private final DeleteProductUseCase deleteProduct;

    public ProductRestController(ListProductsUseCase listProducts,
                                GetProductUseCase getProduct,
                                CreateProductUseCase createProduct,
                                UpdateProductUseCase updateProduct,
                                DeleteProductUseCase deleteProduct) {
        this.listProducts = listProducts;
        this.getProduct = getProduct;
        this.createProduct = createProduct;
        this.updateProduct = updateProduct;
        this.deleteProduct = deleteProduct;
    }

    @GetMapping
    @Operation(summary = "List products",
            description = "Returns a page of catalog products, optionally filtered by category and free text")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Page of products"))
    public ProductPageResponse list(
            @Parameter(description = "Category slug or category name", example = "unicorns")
            @RequestParam(required = false) String category,
            @Parameter(description = "Free text matched against name and description", example = "unicorn")
            @RequestParam(required = false) String q,
            @Parameter(description = "Zero-based page number", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (1-100)", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        return ProductRestMapper.toPageResponse(listProducts.execute(category, q, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a product by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The requested product"),
            @ApiResponse(responseCode = "404", description = "Product does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ProductResponse getById(
            @Parameter(description = "Product identifier", example = "42") @PathVariable long id) {
        return ProductRestMapper.toResponse(getProduct.execute(id));
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
        Product created = createProduct.execute(ProductRestMapper.toCommand(request));
        return ResponseEntity
                .created(URI.create("/api/v1/products/" + created.id()))
                .body(ProductRestMapper.toResponse(created));
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
            @Parameter(description = "Product identifier", example = "42") @PathVariable long id,
            @Valid @RequestBody ProductUpdateRequest request) {
        return ProductRestMapper.toResponse(updateProduct.execute(id, ProductRestMapper.toCommand(request)));
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
            @Parameter(description = "Product identifier", example = "42") @PathVariable long id) {
        deleteProduct.execute(id);
    }
}
