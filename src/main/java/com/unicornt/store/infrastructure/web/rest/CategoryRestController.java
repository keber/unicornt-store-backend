package com.unicornt.store.infrastructure.web.rest;

import com.unicornt.store.application.usecase.catalog.CreateCategoryUseCase;
import com.unicornt.store.application.usecase.catalog.ListCategoriesUseCase;
import com.unicornt.store.domain.model.Category;
import com.unicornt.store.infrastructure.web.dto.CategoryDtos.CategoryCreateRequest;
import com.unicornt.store.infrastructure.web.dto.CategoryDtos.CategoryResponse;
import com.unicornt.store.infrastructure.web.error.ErrorResponse;
import com.unicornt.store.infrastructure.web.mapper.CategoryRestMapper;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** Product category resource. Reads are public; creation requires the administrator role. */
@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories", description = "Product category management")
public class CategoryRestController {

    private final ListCategoriesUseCase listCategories;
    private final CreateCategoryUseCase createCategory;

    public CategoryRestController(ListCategoriesUseCase listCategories,
                                 CreateCategoryUseCase createCategory) {
        this.listCategories = listCategories;
        this.createCategory = createCategory;
    }

    @GetMapping
    @Operation(summary = "List categories", description = "Returns every product category ordered by name")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "All categories"))
    public List<CategoryResponse> list() {
        return listCategories.execute().stream().map(CategoryRestMapper::toResponse).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a category", description = "Administrator only")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Category created, its URI is in the Location header"),
            @ApiResponse(responseCode = "400", description = "Invalid payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Administrator role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "A category with the same slug already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryCreateRequest request) {
        Category created = createCategory.execute(request.name(), request.slug());
        return ResponseEntity
                .created(URI.create("/api/v1/categories/" + created.id()))
                .body(CategoryRestMapper.toResponse(created));
    }
}
