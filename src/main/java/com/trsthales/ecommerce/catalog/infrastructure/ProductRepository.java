package com.trsthales.ecommerce.catalog.infrastructure;

import com.trsthales.ecommerce.catalog.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.variants WHERE p.slug = :slug")
    Optional<Product> findBySlugWithVariants(@Param("slug") String slug);

    @Query(value = """
        SELECT p.* FROM products p
        WHERE p.active = true AND p.search_vector @@ plainto_tsquery('portuguese', :term)
        ORDER BY ts_rank(p.search_vector, plainto_tsquery('portuguese', :term)) DESC
        """,
        countQuery = """
        SELECT count(p.id) FROM products p
        WHERE p.active = true AND p.search_vector @@ plainto_tsquery('portuguese', :term)
        """,
        nativeQuery = true)
    Page<Product> searchByText(@Param("term") String term, Pageable pageable);

    Page<Product> findByActiveTrue(Pageable pageable);
}
