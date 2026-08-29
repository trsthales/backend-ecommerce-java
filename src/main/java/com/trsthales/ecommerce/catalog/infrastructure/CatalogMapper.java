package com.trsthales.ecommerce.catalog.infrastructure;

import com.trsthales.ecommerce.catalog.api.dto.CatalogDtos.CategoryResponse;
import com.trsthales.ecommerce.catalog.api.dto.CatalogDtos.CreateCategoryRequest;
import com.trsthales.ecommerce.catalog.api.dto.CatalogDtos.CreateProductRequest;
import com.trsthales.ecommerce.catalog.api.dto.CatalogDtos.CreateVariantRequest;
import com.trsthales.ecommerce.catalog.api.dto.CatalogDtos.ProductResponse;
import com.trsthales.ecommerce.catalog.api.dto.CatalogDtos.ProductSummaryResponse;
import com.trsthales.ecommerce.catalog.api.dto.CatalogDtos.ProductVariantResponse;
import com.trsthales.ecommerce.catalog.domain.Category;
import com.trsthales.ecommerce.catalog.domain.Product;
import com.trsthales.ecommerce.catalog.domain.ProductVariant;
import com.trsthales.ecommerce.common.domain.Money;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", imports = {Instant.class, UUID.class})
public interface CatalogMapper {

    @Mapping(target = "id", expression = "java(UUID.randomUUID())")
    @Mapping(target = "createdAt", expression = "java(Instant.now())")
    Category toEntity(CreateCategoryRequest request);

    CategoryResponse toResponse(Category category);

    @Mapping(target = "id", expression = "java(UUID.randomUUID())")
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "variants", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "createdAt", expression = "java(Instant.now())")
    @Mapping(target = "updatedAt", expression = "java(Instant.now())")
    Product toEntity(CreateProductRequest request);

    @Mapping(target = "id", expression = "java(UUID.randomUUID())")
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "priceCurrency", defaultExpression = "java(\"BRL\")")
    @Mapping(target = "createdAt", expression = "java(Instant.now())")
    @Mapping(target = "updatedAt", expression = "java(Instant.now())")
    ProductVariant toEntity(CreateVariantRequest request);

    @Mapping(target = "price", expression = "java(variant.getPrice())")
    ProductVariantResponse toResponse(ProductVariant variant);

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "variants", expression = "java(mapVariants(product.getVariants()))")
    ProductResponse toResponse(Product product);

    default List<ProductVariantResponse> mapVariants(List<ProductVariant> variants) {
        if (variants == null) {
            return Collections.emptyList();
        }
        return variants.stream().map(this::toResponse).toList();
    }

    default ProductSummaryResponse toSummary(Product product) {
        if (product == null) {
            return null;
        }
        int count = product.getVariants() != null ? product.getVariants().size() : 0;
        Money minPrice = (product.getVariants() != null && !product.getVariants().isEmpty())
                ? product.getVariants().stream()
                .map(ProductVariant::getPrice)
                .min(Money::compareTo)
                .orElse(Money.ZERO)
                : Money.ZERO;

        return new ProductSummaryResponse(
                product.getId(),
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getTitle(),
                product.getSlug(),
                product.getDescription(),
                product.isActive(),
                count,
                minPrice,
                product.getCreatedAt()
        );
    }
}
