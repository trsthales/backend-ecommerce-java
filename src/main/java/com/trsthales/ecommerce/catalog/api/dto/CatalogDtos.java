package com.trsthales.ecommerce.catalog.api.dto;

import com.trsthales.ecommerce.common.domain.Money;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CatalogDtos {

    private CatalogDtos() {}

    public record CreateCategoryRequest(
            @NotBlank(message = "Nome é obrigatório")
            String name,

            @NotBlank(message = "Slug é obrigatório")
            String slug,

            String description
    ) {}

    public record CategoryResponse(
            UUID id,
            String name,
            String slug,
            String description,
            Instant createdAt
    ) {}

    public record CreateVariantRequest(
            @NotBlank(message = "SKU é obrigatório")
            String sku,

            @NotBlank(message = "Nome da variação é obrigatório")
            String name,

            @NotNull(message = "Preço é obrigatório")
            @PositiveOrZero(message = "Preço não pode ser negativo")
            BigDecimal priceAmount,

            String priceCurrency,

            String imageUrl
    ) {}

    public record CreateProductRequest(
            @NotNull(message = "ID da categoria é obrigatório")
            UUID categoryId,

            @NotBlank(message = "Título é obrigatório")
            String title,

            @NotBlank(message = "Slug é obrigatório")
            String slug,

            String description,

            @Valid
            List<CreateVariantRequest> variants
    ) {}

    public record ProductVariantResponse(
            UUID id,
            String sku,
            String name,
            Money price,
            String imageUrl,
            boolean active
    ) {}

    public record ProductResponse(
            UUID id,
            UUID categoryId,
            String title,
            String slug,
            String description,
            boolean active,
            List<ProductVariantResponse> variants,
            Instant createdAt
    ) {}

    public record ProductSummaryResponse(
            UUID id,
            UUID categoryId,
            String title,
            String slug,
            String description,
            boolean active,
            int variantCount,
            Money minPrice,
            Instant createdAt
    ) {}
}
