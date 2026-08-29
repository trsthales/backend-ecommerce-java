package com.trsthales.ecommerce.catalog.infrastructure;

import com.trsthales.ecommerce.catalog.domain.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    Optional<ProductVariant> findBySku(String sku);

    boolean existsBySku(String sku);

    @Query("SELECT v FROM ProductVariant v JOIN FETCH v.product p WHERE v.sku = :sku")
    Optional<ProductVariant> findBySkuWithProduct(@Param("sku") String sku);
}
