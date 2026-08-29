# Roadmap de Execução: E-Commerce Backend Reference

```text
LEGENDA:
[ ] Tarefa pendente
[x] Tarefa concluída
🎯 Rastreabilidade de Invariante (INV-001 a INV-007)
🧪 Teste Automatizado Mandatório
```

---

## 🏗️ Fase 0: Setup, PactX & Scaffolding Base

- [ ] **TASK-0.1 | Inicialização do PactX & Governança**
  - **Ação:** Executar `pactx init` na raiz do repositório.
  - **Ação:** Criar `requirements.md` mapeando `REQ-001` a `REQ-007` (derivados de `INV-001` a `INV-007`).
  - **Ação:** Criar ADR inicial em `.ai-context/decisions/DEC-001.md` documentando a adoção do Spring Modulith, Java 21 e PostgreSQL.
  - **Critério de Aceite:** `pactx doctor` reportando `0 errors, 0 warnings`.

- [x] **TASK-0.2 | Configuração do Build (`pom.xml` / `build.gradle.kts`)**
  - **Ação:** Configurar Java 21, Spring Boot 3.4+, Spring Modulith Starter (BOM), PostgreSQL Driver, Flyway Core, Testcontainers (Postgres, Redis), MapStruct, Lombok e Spring Security OAuth2 Resource Server.
  - **Critério de Aceite:** Compilação limpa via `mvn clean compile`.

- [x] **TASK-0.3 | Ambiente de Desenvolvimento Local (`docker-compose.yml`)**
  - **Ação:** Provisionar PostgreSQL 16 (porta 5432), Redis 7 Alpine (porta 6379) e MinIO S3-Compatible (portas 9000/9001).
  - **Critério de Aceite:** Containers subindo saudáveis via `docker compose up -d`.

- [x] **TASK-0.4 | Infraestrutura Base de Testes de Integração**
  - **Arquivo:** `src/test/java/com/trsthales/ecommerce/AbstractIntegrationTest.java`
  - **Ação:** Criar classe base singleton para inicialização dos containers PostgreSQL e Redis via Testcontainers com `@DynamicPropertySource`.
  - **Critério de Aceite:** Um teste `@SpringBootTest` de *smoke test* executando com sucesso contra os containers.

---

## 🧩 Fase 1: Kernel Compartilhado (`common`)

- [x] **TASK-1.1 | Value Object `Money` & Algoritmo do Maior Resto 🎯 INV-004**
  - **Arquivo:** `com.trsthales.ecommerce.common.domain.Money.java`
  - **Ação:** Implementar VO imutável com `BigDecimal`, validação de moeda ISO-4217, operações aritméticas e o método `distribute(List<BigDecimal> weights)` (Hare-Niemeyer Method).
  - 🧪 **Teste:** `MoneyTest.java` validando rateios ímpares (ex: R$ 10,00 entre 3 itens de peso igual $\to$ R$ 3,34 + R$ 3,33 + R$ 3,33 = R$ 10,00 exatos).

- [x] **TASK-1.2 | Contrato Agnóstico de Usuário Autenticado 🎯 INV-006**
  - **Arquivos:**
    - `com.trsthales.ecommerce.common.security.AuthenticatedUser.java`
    - `com.trsthales.ecommerce.common.security.CurrentUserProvider.java`
  - **Ação:** Definir o record imutável de identidade e a interface de acesso ao `SecurityContextHolder`.

- [x] **TASK-1.3 | Motor Universal de Idempotência & Crash Recovery 🎯 INV-003**
  - **Arquivos:**
    - `src/main/resources/db/migration/common/V1__create_idempotency_keys.sql`
    - `com.trsthales.ecommerce.common.idempotency.IdempotencyFilter.java`
    - `com.trsthales.ecommerce.common.idempotency.IdempotencyKeyRepository.java`
  - **Ação:** Criar tabela `idempotency_keys` com `locked_until`, interceptor de request e rotina de liberação de chave travada após timeout de 2 minutos (*crash recovery*).
  - 🧪 **Teste:** `IdempotencyEngineIntegrationTest.java` simulando replay de requisições e recuperação de chaves travadas.

- [x] **TASK-1.4 | Tratamento Uniforme de Erros (RFC 7807)**
  - **Arquivos:**
    - `com.trsthales.ecommerce.common.exception.ProblemDetailsAdvice.java`
    - `com.trsthales.ecommerce.common.exception.DomainException.java`
  - **Ação:** Padronizar retornos de erro (`400`, `404`, `409`, `422`, `500`) no formato JSON `ProblemDetail`.

---

## 🔐 Fase 2: Identidade & Segurança (`identity`)

- [x] **TASK-2.1 | Schema e Entidades de Identidade**
  - **Arquivos:**
    - `src/main/resources/db/migration/V2__create_identity_tables.sql`
    - `com.trsthales.ecommerce.identity.domain.User.java`
    - `com.trsthales.ecommerce.identity.domain.Role.java`
  - **Ação:** Criar tabelas `users`, `roles`, `user_roles` com hash de senha via BCrypt.

- [x] **TASK-2.2 | Provedor de Chaves RSA & Token Service**
  - **Arquivos:**
    - `com.trsthales.ecommerce.identity.infrastructure.RsaKeyProvider.java`
    - `com.trsthales.ecommerce.identity.application.TokenService.java`
  - **Ação:** Configurar par de chaves RSA assimétricas para assinatura (Private Key) e validação (Public Key) de tokens JWT com claims de `userId`, `email` e `roles`.

- [x] **TASK-2.3 | Configuração de Segurança Spring & Bridge de Contexto**
  - **Arquivos:**
    - `com.trsthales.ecommerce.identity.infrastructure.SecurityConfig.java`
    - `com.trsthales.ecommerce.identity.infrastructure.JwtAuthenticationConverter.java`
  - **Ação:** Configurar `SecurityFilterChain` OAuth2 Resource Server convertendo JWTs validados diretamente no `AuthenticatedUser` do módulo `common`.

- [x] **TASK-2.4 | Endpoints de Autenticação & Registro**
  - **Arquivos:**
    - `com.trsthales.ecommerce.identity.api.AuthController.java`
    - `com.trsthales.ecommerce.identity.api.dto.RegisterUserRequest.java`
    - `com.trsthales.ecommerce.identity.api.dto.LoginRequest.java`
  - **Ação:** Implementar rotas públicas `POST /api/v1/auth/register` e `POST /api/v1/auth/token`.
  - 🧪 **Teste:** `AuthIntegrationTest.java` cobrindo registro, login, expiração e rejeição de tokens adulterados.

---

## 📦 Fase 3: Catálogo, Busca & Imagens (`catalog`)

- [ ] **TASK-3.1 | Schema do Catálogo com Variações de SKU**
  - **Arquivos:**
    - `src/main/resources/db/migration/catalog/V3__create_catalog_tables.sql`
    - `com.trsthales.ecommerce.catalog.domain.Category.java`
    - `com.trsthales.ecommerce.catalog.domain.Product.java`
    - `com.trsthales.ecommerce.catalog.domain.ProductVariant.java`
  - **Ação:** Modelar catálogo com suporte a SKU, atributos (cor/tamanho) e preço de tabela.

- [ ] **TASK-3.2 | Busca Full-Text com PostgreSQL (GIN Index)**
  - **Ação:** Criar coluna gerada `search_vector (tsvector)` indexada com GIN em `products`, com queries nativas de ranking por relevância.

- [ ] **TASK-3.3 | Adapter de Armazenamento de Imagens S3/MinIO**
  - **Arquivos:**
    - `com.trsthales.ecommerce.catalog.infrastructure.ImageStorageService.java`
    - `com.trsthales.ecommerce.catalog.infrastructure.S3ImageStorageAdapter.java`
  - **Ação:** Upload e geração de URLs públicas de imagens via MinIO / S3.

- [ ] **TASK-3.4 | API Pública do Módulo Catálogo 🎯 INV-006**
  - **Arquivo:** `com.trsthales.ecommerce.catalog.api.CatalogQueryService.java`
  - **Ação:** Expor interface `@NamedInterface` para que outros módulos consultem produtos sem tocar em repositórios JPA alheios.
  - 🧪 **Teste:** `CatalogIntegrationTest.java` cobrindo busca textual e upload no MinIO via Testcontainers.

---

## 📊 Fase 4: Inventário, Concorrência & Reservas (`inventory`)

- [ ] **TASK-4.1 | Schema de Inventário & Constraints de Defesa 🎯 INV-002, INV-007**
  - **Arquivos:**
    - `src/main/resources/db/migration/inventory/V4__create_inventory_tables.sql`
    - `com.trsthales.ecommerce.inventory.domain.InventoryItem.java`
    - `com.trsthales.ecommerce.inventory.domain.InventoryReservation.java`
  - **Ação:** Criar tabelas com `CHECK (physical_quantity >= 0)` e `CHECK (reserved_quantity <= physical_quantity)`.

- [ ] **TASK-4.2 | Repositório com Locking Pessimista**
  - **Arquivo:** `com.trsthales.ecommerce.inventory.infrastructure.InventoryItemRepository.java`
  - **Ação:** Implementar `findBySkuForUpdate` usando `@Lock(LockModeType.PESSIMISTIC_WRITE)`.

- [ ] **TASK-4.3 | Casos de Uso de Reserva e Baixa**
  - **Arquivo:** `com.trsthales.ecommerce.inventory.application.InventoryService.java`
  - **Ação:** Métodos `reserve(sku, qty, orderId, ttl)`, `fulfill(reservationId)` e `release(reservationId)`.

- [ ] **TASK-4.4 | Listener de Compensação de Expiração**
  - **Arquivo:** `com.trsthales.ecommerce.inventory.events.OrderExpiredEventListener.java`
  - **Ação:** Ouvir `OrderExpiredEvent` via `@ApplicationModuleListener` e disparar a liberação de saldo.
  - 🧪 **Teste Mandatório (Milestone):** `InventoryConcurrencyIntegrationTest.java` disparando **20 threads concorrentes disputando 1 item unitário** contra o PostgreSQL via Testcontainers.

---

## 🛒 Fase 5: Carrinho Volátil no Redis (`cart`)

- [ ] **TASK-5.1 | Modelagem e Serialização do Carrinho no Redis**
  - **Arquivos:**
    - `com.trsthales.ecommerce.cart.domain.Cart.java`
    - `com.trsthales.ecommerce.cart.domain.CartItem.java`
    - `com.trsthales.ecommerce.cart.infrastructure.RedisCartRepository.java`
  - **Ação:** Persistir carrinho com TTL automático de 7 dias atrelado ao `userId` ou `sessionId`.

- [ ] **TASK-5.2 | Pre-flight Check de Disponibilidade**
  - **Arquivo:** `com.trsthales.ecommerce.cart.application.CartService.java`
  - **Ação:** Ao consultar o carrinho (`GET /api/v1/cart`), cruzar SKUs com o `InventoryQueryService` e sinalizar itens esgotados sem efetuar reserva.

- [ ] **TASK-5.3 | Endpoints REST do Carrinho**
  - **Arquivo:** `com.trsthales.ecommerce.cart.api.CartController.java`
  - **Ação:** CRUD completo do carrinho com validações Jakarta.
  - 🧪 **Teste:** `CartRedisIntegrationTest.java` validando TTL, expiração e pre-flight check com Redis Testcontainers.

---

## 🏷️ Fase 6: Promoções & Frete (`promotions`, `shipping`)

- [ ] **TASK-6.1 | Schema e Agregado de Cupons**
  - **Arquivos:**
    - `src/main/resources/db/migration/promotions/V6__create_promotions_tables.sql`
    - `com.trsthales.ecommerce.promotions.domain.Coupon.java`
    - `com.trsthales.ecommerce.promotions.application.PromotionService.java`
  - **Ação:** Regras de cupom percentual/fixo, valor mínimo de pedido, validade e limite de usos.

- [ ] **TASK-6.2 | SPI de Cálculo de Frete**
  - **Arquivos:**
    - `com.trsthales.ecommerce.shipping.domain.ShippingCalculator.java`
    - `com.trsthales.ecommerce.shipping.infrastructure.MockShippingCalculator.java`
  - **Ação:** Cálculo de prazos e valores de frete por CEP de destino, peso e dimensões.
  - 🧪 **Teste:** `PromotionAndShippingTest.java` validando regras de desconto e cálculo de frete.

---

## 📋 Fase 7: Pedidos & Checkout Transacional (`orders`)

- [ ] **TASK-7.1 | Schema de Pedidos & Snapshot Histórico 🎯 INV-004**
  - **Arquivos:**
    - `src/main/resources/db/migration/orders/V7__create_orders_tables.sql`
    - `com.trsthales.ecommerce.orders.domain.Order.java`
    - `com.trsthales.ecommerce.orders.domain.OrderItemSnapshot.java`
  - **Ação:** Criar tabelas `orders` e `order_items` **sem FK com o catálogo**, gravando `grossUnitPrice`, `discountAmount`, `netUnitPrice` e `quantity`.

- [ ] **TASK-7.2 | Máquina de Estados Estrita do Pedido 🎯 INV-005**
  - **Arquivo:** `com.trsthales.ecommerce.orders.domain.OrderStatus.java`
  - **Ação:** Implementar métodos semânticos de transição de estado (`markAsPaid()`, `cancel()`, `ship()`, `deliver()`) rejeitando transições ilegais.

- [ ] **TASK-7.3 | Transação 1: Orquestração do Checkout**
  - **Arquivo:** `com.trsthales.ecommerce.orders.application.CheckoutService.java`
  - **Ação:** Executar em uma única transação JDBC:
    1. Validar carrinho e aplicar cupom via `Money.distribute()`.
    2. Reservar estoque no módulo `inventory`.
    3. Persistir agregador `Order` em `PAYMENT_PENDING`.
    4. Publicar `OrderCreatedEvent` no Outbox do Spring Modulith.
    5. Limpar carrinho no Redis.

- [ ] **TASK-7.4 | Worker de Pedidos Expirados**
  - **Arquivo:** `com.trsthales.ecommerce.orders.infrastructure.OrderExpirationJob.java`
  - **Ação:** Job `@Scheduled` buscando pedidos em `PAYMENT_PENDING` com mais de 15 minutos e publicando `OrderExpiredEvent`.
  - 🧪 **Teste:** `OrderCheckoutIntegrationTest.java` validando o snapshot imutável, o rateio pro-rata e a expiração automática.

---

## 💳 Fase 8: Pagamentos, Webhooks & Reconciliação (`payments`)

- [ ] **TASK-8.1 | SPI de Provedores de Pagamento**
  - **Arquivos:**
    - `com.trsthales.ecommerce.payments.domain.PaymentProvider.java`
    - `com.trsthales.ecommerce.payments.infrastructure.FakePaymentProvider.java`
  - **Ação:** Interface SPI de processamento e adapter em memória para testes determinísticos.

- [ ] **TASK-8.2 | Ingestão Idempotente de Webhooks 🎯 INV-003**
  - **Arquivos:**
    - `src/main/resources/db/migration/payments/V8__create_payments_tables.sql`
    - `com.trsthales.ecommerce.payments.api.PaymentWebhookController.java`
    - `com.trsthales.ecommerce.payments.application.WebhookIngestionService.java`
  - **Ação:** Tabela `payment_webhook_events` com validação de `payload_hash`. Descartar duplicatas com `200 OK`; rejeitar divergência de payload com `422 Unprocessable Entity`.

- [ ] **TASK-8.3 | Transação 2: Confirmação Atômica de Pagamento 🎯 INV-001**
  - **Arquivo:** `com.trsthales.ecommerce.payments.application.PaymentConfirmationService.java`
  - **Ação:** Executar SQL condicional:
    ```sql
    UPDATE orders SET status = 'PAID', version = version + 1 
    WHERE id = :id AND status = 'PAYMENT_PENDING'
    ```
    Confirmar reserva de estoque no `inventory` (`FULFILLED`) e publicar `OrderPaidEvent` no Outbox.

- [ ] **TASK-8.4 | Reconciliador Ativo de Pagamentos**
  - **Arquivo:** `com.trsthales.ecommerce.payments.infrastructure.PaymentReconciliationJob.java`
  - **Ação:** Job `@Scheduled` consultando ativamente o `PaymentProvider` para pedidos pendentes há mais de 10 minutos.
  - 🧪 **Teste Mandatório (Milestone):** `PaymentWebhookConcurrencyIntegrationTest.java` com **10 webhooks simultâneos para o mesmo pedido** garantindo exatamente 1 transição para `PAID`.

---

## 🏛️ Fase 9: Verificação Modulith, Observabilidade & Fechamento

- [ ] **TASK-9.1 | Teste Estrutural de Arquitetura Spring Modulith 🎯 INV-006**
  - **Arquivo:** `src/test/java/com/trsthales/ecommerce/ModularityArchitectureTest.java`
  - **Ação:** Executar `ApplicationModules.of(EcommerceApplication.class).verify()` garantindo **zero dependências circulares ou acessos indevidos a pacotes internos**.

- [ ] **TASK-9.2 | Documentação de Arquitetura & Diagramas C4**
  - **Ação:** Gerar diagramas PlantUML / C4 dos módulos automaticamente via `new Documenter(modules).writeModulesAsPlantUml()`.

- [ ] **TASK-9.3 | Documentação Interativa de APIs (OpenAPI / Swagger)**
  - **Ação:** Configurar `springdoc-openapi` expondo a documentação interativa em `/swagger-ui.html`.

- [ ] **TASK-9.4 | Fechamento de Contexto no PactX**
  - **Ação:** Executar `pactx doctor --fix` para sincronizar todas as ADRs (`decisions/DEC-001.md` a `DEC-008.md`) e requisitos atendidos.
  - **Ação:** Executar `pactx pack` para gerar o snapshot final do projeto.
  - 🧪 **Validação Final:** `mvn clean verify` com 100% dos testes de unidade, integração e concorrência passando em pipeline hermético.

---

### Início da Execução

Estamos com o plano traçado e a lista de tarefas mapeada. 

Para iniciar a **Fase 0 (TASK-0.1 a TASK-0.4)** e a **Fase 1 (TASK-1.1 a TASK-1.4)**, podemos gerar:
1. O arquivo `pom.xml` canônico completo;
2. O `docker-compose.yml`;
3. A classe base `AbstractIntegrationTest`;
4. A implementação completa do Value Object `Money` com os testes unitários do Algoritmo do Maior Resto.

Avançamos com a geração desse primeiro bloco de código?