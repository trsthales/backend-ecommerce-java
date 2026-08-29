package com.trsthales.ecommerce.catalog.application;

import com.trsthales.ecommerce.catalog.api.CatalogQueryService;
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
import com.trsthales.ecommerce.catalog.infrastructure.CatalogMapper;
import com.trsthales.ecommerce.catalog.infrastructure.CategoryRepository;
import com.trsthales.ecommerce.catalog.infrastructure.ProductRepository;
import com.trsthales.ecommerce.catalog.infrastructure.ProductVariantRepository;
import com.trsthales.ecommerce.common.exception.BusinessRuleViolationException;
import com.trsthales.ecommerce.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CatalogService implements CatalogQueryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CatalogMapper catalogMapper;

    public CatalogService(
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            ProductVariantRepository variantRepository,
            CatalogMapper catalogMapper
    ) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.catalogMapper = catalogMapper;
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        String slug = request.slug().toLowerCase().trim();
        if (categoryRepository.existsBySlug(slug)) {
            throw new BusinessRuleViolationException("Categoria com slug já existente: " + slug);
        }

        Category category = catalogMapper.toEntity(request);
        category.setSlug(slug);
        Category saved = categoryRepository.save(category);
        return catalogMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        return categoryRepository.findAll().stream()
                .map(catalogMapper::toResponse)
                .toList();
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada: " + request.categoryId()));

        String slug = request.slug().toLowerCase().trim();
        if (productRepository.existsBySlug(slug)) {
            throw new BusinessRuleViolationException("Produto com slug já existente: " + slug);
        }

        Product product = catalogMapper.toEntity(request);
        product.setSlug(slug);
        product.setCategory(category);

        if (request.variants() != null) {
            for (CreateVariantRequest vReq : request.variants()) {
                if (variantRepository.existsBySku(vReq.sku())) {
                    throw new BusinessRuleViolationException("SKU já cadastrado: " + vReq.sku());
                }
                ProductVariant variant = catalogMapper.toEntity(vReq);
                product.addVariant(variant);
            }
        }

        Product saved = productRepository.save(product);
        return catalogMapper.toResponse(saved);
    }

    @Transactional
    public ProductVariantResponse addVariant(UUID productId, CreateVariantRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado: " + productId));

        if (variantRepository.existsBySku(request.sku())) {
            throw new BusinessRuleViolationException("SKU já cadastrado: " + request.sku());
        }

        ProductVariant variant = catalogMapper.toEntity(request);
        product.addVariant(variant);
        ProductVariant saved = variantRepository.save(variant);

        return catalogMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlugWithVariants(slug.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com o slug: " + slug));

        return catalogMapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> listProducts(String query, Pageable pageable) {
        if (query != null && !query.isBlank()) {
            return productRepository.searchByText(query.trim(), pageable)
                    .map(catalogMapper::toSummary);
        }
        return productRepository.findByActiveTrue(pageable)
                .map(catalogMapper::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductVariantView> findVariantBySku(String sku) {
        return variantRepository.findBySkuWithProduct(sku)
                .map(v -> new ProductVariantView(
                        v.getId(),
                        v.getProduct().getId(),
                        v.getSku(),
                        v.getProduct().getTitle(),
                        v.getName(),
                        v.getPrice(),
                        v.isActive() && v.getProduct().isActive()
                ));
    }
}
