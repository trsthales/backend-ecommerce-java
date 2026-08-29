package com.trsthales.ecommerce.catalog.api;

import com.trsthales.ecommerce.catalog.api.dto.CatalogDtos.CreateProductRequest;
import com.trsthales.ecommerce.catalog.api.dto.CatalogDtos.CreateVariantRequest;
import com.trsthales.ecommerce.catalog.api.dto.CatalogDtos.ProductResponse;
import com.trsthales.ecommerce.catalog.api.dto.CatalogDtos.ProductSummaryResponse;
import com.trsthales.ecommerce.catalog.api.dto.CatalogDtos.ProductVariantResponse;
import com.trsthales.ecommerce.catalog.application.CatalogService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final CatalogService catalogService;

    public ProductController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public ResponseEntity<Page<ProductSummaryResponse>> listProducts(
            @RequestParam(value = "query", required = false) String query,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<ProductSummaryResponse> response = catalogService.listProducts(query, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ProductResponse> getProductBySlug(@PathVariable("slug") String slug) {
        ProductResponse response = catalogService.getProductBySlug(slug);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse response = catalogService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/variants")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ProductVariantResponse> addVariant(
            @PathVariable("id") UUID id,
            @Valid @RequestBody CreateVariantRequest request
    ) {
        ProductVariantResponse response = catalogService.addVariant(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
