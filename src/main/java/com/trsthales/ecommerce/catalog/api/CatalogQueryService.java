package com.trsthales.ecommerce.catalog.api;

import com.trsthales.ecommerce.common.domain.Money;
import org.springframework.modulith.NamedInterface;

import java.util.Optional;
import java.util.UUID;

@NamedInterface
public interface CatalogQueryService {

    Optional<ProductVariantView> findVariantBySku(String sku);

    record ProductVariantView(
            UUID variantId,
            UUID productId,
            String sku,
            String productTitle,
            String variantName,
            Money price,
            boolean active
    ) {
    }
}
