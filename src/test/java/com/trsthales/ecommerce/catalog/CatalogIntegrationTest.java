package com.trsthales.ecommerce.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trsthales.ecommerce.AbstractIntegrationTest;
import com.trsthales.ecommerce.catalog.api.CatalogQueryService;
import com.trsthales.ecommerce.catalog.api.dto.CatalogDtos.CreateCategoryRequest;
import com.trsthales.ecommerce.catalog.api.dto.CatalogDtos.CreateProductRequest;
import com.trsthales.ecommerce.catalog.api.dto.CatalogDtos.CreateVariantRequest;
import com.trsthales.ecommerce.catalog.infrastructure.CategoryRepository;
import com.trsthales.ecommerce.catalog.infrastructure.ProductRepository;
import com.trsthales.ecommerce.catalog.infrastructure.ProductVariantRepository;
import com.trsthales.ecommerce.common.domain.Money;
import com.trsthales.ecommerce.identity.application.TokenService;
import com.trsthales.ecommerce.identity.domain.Role;
import com.trsthales.ecommerce.identity.domain.User;
import com.trsthales.ecommerce.identity.infrastructure.RoleRepository;
import com.trsthales.ecommerce.identity.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("Catalog Module Integration Tests (Fase 3: TASK-3.1 a TASK-3.4)")
class CatalogIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private CatalogQueryService catalogQueryService;

    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        Role roleAdmin = roleRepository.findById(Role.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(Role.ROLE_ADMIN, "Admin")));
        Role roleCustomer = roleRepository.findById(Role.ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(new Role(Role.ROLE_CUSTOMER, "Customer")));

        Instant now = Instant.now();
        User admin = User.builder()
                .id(UUID.randomUUID())
                .email("admin@ecommerce.com")
                .passwordHash(passwordEncoder.encode("AdminPass123!"))
                .fullName("Admin User")
                .enabled(true)
                .roles(Set.of(roleAdmin))
                .createdAt(now)
                .updatedAt(now)
                .build();
        userRepository.save(admin);
        adminToken = tokenService.generateToken(admin).accessToken();

        User customer = User.builder()
                .id(UUID.randomUUID())
                .email("customer@ecommerce.com")
                .passwordHash(passwordEncoder.encode("CustomerPass123!"))
                .fullName("Customer User")
                .enabled(true)
                .roles(Set.of(roleCustomer))
                .createdAt(now)
                .updatedAt(now)
                .build();
        userRepository.save(customer);
        customerToken = tokenService.generateToken(customer).accessToken();
    }

    @Test
    @DisplayName("Should create category and product with variants as ADMIN")
    void shouldCreateCategoryAndProductWithVariants() throws Exception {
        // 1. Create Category
        CreateCategoryRequest categoryReq = new CreateCategoryRequest(
                "Eletrônicos",
                "eletronicos",
                "Dispositivos eletrônicos e gadgets"
        );

        MvcResult catResult = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.slug", is("eletronicos")))
                .andReturn();

        UUID categoryId = UUID.fromString(objectMapper.readTree(catResult.getResponse().getContentAsString()).get("id").asText());

        // 2. Create Product with initial Variant
        CreateVariantRequest variant1 = new CreateVariantRequest(
                "NB-DELL-16GB",
                "Dell XPS 13 16GB RAM",
                new BigDecimal("7999.90"),
                "BRL",
                "https://cdn.ecommerce.com/dell-16gb.jpg"
        );

        CreateProductRequest productReq = new CreateProductRequest(
                categoryId,
                "Notebook Dell XPS 13",
                "notebook-dell-xps-13",
                "Notebook ultraportátil com tela InfinityEdge e processador potente",
                List.of(variant1)
        );

        MvcResult prodResult = mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.slug", is("notebook-dell-xps-13")))
                .andExpect(jsonPath("$.variants", hasSize(1)))
                .andExpect(jsonPath("$.variants[0].sku", is("NB-DELL-16GB")))
                .andExpect(jsonPath("$.variants[0].price.amount", is(7999.90)))
                .andReturn();

        UUID productId = UUID.fromString(objectMapper.readTree(prodResult.getResponse().getContentAsString()).get("id").asText());

        // 3. Add second Variant to existing Product
        CreateVariantRequest variant2 = new CreateVariantRequest(
                "NB-DELL-32GB",
                "Dell XPS 13 32GB RAM",
                new BigDecimal("9499.00"),
                "BRL",
                "https://cdn.ecommerce.com/dell-32gb.jpg"
        );

        mockMvc.perform(post("/api/v1/products/" + productId + "/variants")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(variant2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku", is("NB-DELL-32GB")))
                .andExpect(jsonPath("$.price.amount", is(9499.00)));

        // 4. Public GET by slug
        mockMvc.perform(get("/api/v1/products/notebook-dell-xps-13"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Notebook Dell XPS 13")))
                .andExpect(jsonPath("$.variants", hasSize(2)));
    }

    @Test
    @DisplayName("Should perform PostgreSQL Full-Text Search with GIN index")
    void shouldPerformPostgreSqlFullTextSearch() throws Exception {
        CreateCategoryRequest categoryReq = new CreateCategoryRequest("Smartphones", "smartphones", "Celulares");
        MvcResult catResult = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryReq)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID categoryId = UUID.fromString(objectMapper.readTree(catResult.getResponse().getContentAsString()).get("id").asText());

        // Product 1: Smartphone Galaxy
        CreateProductRequest prod1 = new CreateProductRequest(
                categoryId,
                "Smartphone Samsung Galaxy Ultra",
                "samsung-galaxy-ultra",
                "Excelente celular com câmera de 200MP e tela AMOLED",
                List.of(new CreateVariantRequest("GAL-ULTRA-256", "Preto 256GB", new BigDecimal("5499.00"), "BRL", null))
        );

        // Product 2: Fone de Ouvido Bluetooth
        CreateProductRequest prod2 = new CreateProductRequest(
                categoryId,
                "Fone de Ouvido Bluetooth Pro",
                "fone-bluetooth-pro",
                "Fone com cancelamento ativo de ruído para música",
                List.of(new CreateVariantRequest("FONE-BT-01", "Branco", new BigDecimal("499.00"), "BRL", null))
        );

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prod1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prod2)))
                .andExpect(status().isCreated());

        // Search for "celular" (Portuguese stemming should match description "celular")
        mockMvc.perform(get("/api/v1/products?query=celular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].slug", is("samsung-galaxy-ultra")));

        // Search for "ruído"
        mockMvc.perform(get("/api/v1/products?query=ruido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].slug", is("fone-bluetooth-pro")));
    }

    @Test
    @DisplayName("Should query variant through CatalogQueryService (@NamedInterface)")
    void shouldQueryVariantThroughCatalogQueryService() throws Exception {
        CreateCategoryRequest catReq = new CreateCategoryRequest("Gamer", "gamer", "Equipamentos Gamer");
        MvcResult catRes = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(catReq)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID categoryId = UUID.fromString(objectMapper.readTree(catRes.getResponse().getContentAsString()).get("id").asText());

        CreateProductRequest prodReq = new CreateProductRequest(
                categoryId,
                "Teclado Mecânico RGB",
                "teclado-mecanico-rgb",
                "Teclado gamer switches azuis",
                List.of(new CreateVariantRequest("KB-RGB-BLUE", "Switch Azul", new BigDecimal("350.00"), "BRL", null))
        );

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prodReq)))
                .andExpect(status().isCreated());

        // Modulith inter-module contract call
        Optional<CatalogQueryService.ProductVariantView> variantView = catalogQueryService.findVariantBySku("KB-RGB-BLUE");

        assertThat(variantView).isPresent();
        CatalogQueryService.ProductVariantView view = variantView.get();
        assertThat(view.sku()).isEqualTo("KB-RGB-BLUE");
        assertThat(view.productTitle()).isEqualTo("Teclado Mecânico RGB");
        assertThat(view.variantName()).isEqualTo("Switch Azul");
        assertThat(view.price()).isEqualTo(Money.of("350.00"));
        assertThat(view.active()).isTrue();
    }

    @Test
    @DisplayName("Should reject product creation for CUSTOMER with 403 Forbidden")
    void shouldRejectCustomerCreatingProduct() throws Exception {
        CreateCategoryRequest categoryReq = new CreateCategoryRequest("Teste", "teste", "Desc");

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should reject product creation for unauthenticated user with 401 Unauthorized")
    void shouldRejectUnauthenticatedCreatingProduct() throws Exception {
        CreateCategoryRequest categoryReq = new CreateCategoryRequest("Teste", "teste", "Desc");

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryReq)))
                .andExpect(status().isUnauthorized());
    }
}
