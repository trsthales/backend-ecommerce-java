Aqui está o **Plano de Implementação da RFC-001**, estruturado em fases incrementais (*Milestones*), com critérios estritos de *Definition of Done (DoD)* e vínculo direto de rastreabilidade com as invariantes (`INV-001` a `INV-007`).

---

# Plano de Implementação: E-Commerce Backend Reference

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│                               GRAFO DE EXECUÇÃO                                  │
├──────────────────────────────────────────────────────────────────────────────────┤
│ Fase 0: Setup, PactX & Scaffolding                                               │
│   └──> Fase 1: Kernel Compartilhado (`common`)                                   │
│          ├──> Fase 2: Identidade & Segurança RSA (`identity`)                    │
│          ├──> Fase 3: Catálogo, FTS & Storage (`catalog`)                        │
│          ├──> Fase 4: Inventário & Concorrência (`inventory` [INV-002, INV-007])│
│          │      └──> Fase 5: Carrinho Volátil no Redis (`cart`)                  │
│          ├──> Fase 6: Promoções & Frete (`promotions`, `shipping`)               │
│          └──> Fase 7: Pedidos & Checkout Transacional (`orders` [INV-004, INV-005])│
│                 └──> Fase 8: Pagamentos, Webhooks & SPI (`payments` [INV-001, 003])│
│                        └──> Fase 9: Verificação Modulith & Hardening             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## Fase 0: Governança, Scaffolding & CI Baseline
- **Objetivo:** Estabelecer o repositório, governança de contexto com PactX e pipeline de testes herméticos com Docker/Testcontainers.

### Entregáveis:
1. **Governança PactX:**
   - Executar `pactx init`.
   - Cadastrar requisitos `REQ-001` a `REQ-007` correspondentes às invariantes `INV-001` a `INV-007` em `requirements.md`.
   - Criar `DEC-001.md`: Adoção de Modular Monolith com Spring Modulith e Java 21.
2. **Setup do Projeto Spring Boot:**
   - Java 21, Spring Boot 3.4+, Maven/Gradle.
   - Dependências: Spring Web, Spring Data JPA, Spring Security, Spring Modulith (Starter + Starter Test), PostgreSQL Driver, Flyway, Testcontainers (Postgres, Redis), MapStruct, Lombok.
3. **Ambiente Local (Docker Compose):**
   - `docker-compose.yml` contendo: PostgreSQL 16, Redis 7 (Alpine), MinIO (S3-compatible).
4. **Base de Testes de Integração:**
   - Classe abstrata `AbstractIntegrationTest` configurando Testcontainers singleton reutilizáveis.

- **DoD:** `mvn clean verify` executando com sucesso e subindo containers locais.

---

## Fase 1: Kernel Compartilhado (`common`)
- **Objetivo:** Implementar os contratos fundamentais de dinheiro, segurança desacoplada e idempotência à prova de crash.

### Entregáveis:
1. **Value Object `Money` (`INV-004`):**
   - Implementação de `Money` com `BigDecimal` e o método `distribute(List<BigDecimal> weights)` usando o **Algoritmo do Maior Resto (*Hare-Niemeyer*)**.
   - Suíte de testes unitários com 100% de cobertura (cenários de divisão ímpar, centavos residuais, soma exata).
2. **Contratos de Segurança Desacoplada (`INV-006`):**
   - Record `AuthenticatedUser(UUID userId, String email, Set<String> roles)`.
   - Interface `CurrentUserProvider` com implementação baseada no `SecurityContextHolder`.
3. **Motor Universal de Idempotência (`INV-003`):**
   - Migration Flyway `V1__create_idempotency_keys_table.sql`.
   - Handler/Interceptor transacional de `Idempotency-Key` com tratamento de lease timeout (recuperação pós-crash de 2 minutos).
4. **Tratamento Global de Exceções (RFC 7807):**
   - `@RestControllerAdvice` padronizando `ProblemDetail` para erros de validação, conflitos e exceções de domínio.

- **DoD:** Teste de crash recovery de idempotência e teste de exatidão monetária passando.

---

## Fase 2: Identidade & Segurança (`identity`)
- **Objetivo:** Autenticação stateless via RSA Asymmetric JWT e RBAC.

### Entregáveis:
1. **Modelagem & Persistência:**
   - Entidades `User`, `Role` (`CUSTOMER`, `ADMIN`).
   - Migration Flyway isolada para tabelas de identidade.
2. **Segurança RSA (OAuth2 Resource Server):**
   - Provedor de chaves assimétricas RSA (geração ou leitura de PEM).
   - Configuração de `SecurityFilterChain` pública para login/registro e autenticada para endpoints de domínio.
   - Endpoint `POST /api/v1/auth/token` e `POST /api/v1/auth/register`.
3. **Bridge com o Kernel:**
   - Mapper do JWT validado para o `AuthenticatedUser` no `SecurityContext`.

- **DoD:** Testes de integração validando emissão, expiração e rejeição de tokens forjados.

---

## Fase 3: Catálogo, Busca & Imagens (`catalog`)
- **Objetivo:** Gestão de produtos, variações (SKUs), busca textual eficiente e imagens.

### Entregáveis:
1. **Modelagem:**
   - `Category`, `Product`, `ProductVariant` (SKU, atributos como cor/tamanho).
2. **Busca Textual no Postgres:**
   - Migration Flyway com coluna `tsvector` e índice GIN para busca performática por título e descrição.
3. **Storage S3/MinIO:**
   - Adapter de upload de imagens usando SDK S3 compatível com MinIO local e Cloudflare R2 / AWS S3 em produção.
4. **API Pública do Módulo:**
   - `CatalogQueryService` exportado como `@NamedInterface` para consulta de outros módulos sem acesso direto ao JPA.

- **DoD:** Teste de busca Full-Text e upload de imagem no MinIO via Testcontainers.

---

## Fase 4: Inventário & Concorrência (`inventory`)
- **Objetivo:** Garantir saldo físico não-negativo (`INV-002`) e ciclo de vida de reserva (`INV-007`).

### Entregáveis:
1. **Modelagem & Constraints:**
   - `InventoryItem` com `CHECK (physical_quantity >= 0)` e `CHECK (reserved_quantity <= physical_quantity)`.
   - `InventoryReservation` (`ACTIVE`, `FULFILLED`, `RELEASED`) com TTL de 15 minutos.
2. **Locking Pessimista:**
   - `findBySkuForUpdate` executando `SELECT ... FOR UPDATE`.
3. **Casos de Uso:**
   - `reserveStock()`, `fulfillReservation()`, `releaseReservation()`.
4. **Listener de Compensação:**
   - `@ApplicationModuleListener` ouvindo `OrderExpiredEvent` e liberando reservas automaticamente.

- **DoD (Milestone Test):** Teste multithread com 20 requisições simultâneas disputando 1 item unitário no Postgres, garantindo exatamente 1 reserva e 19 erros sem inconsistência.

---

## Fase 5: Carrinho Volátil (`cart`)
- **Objetivo:** Carrinho efêmero no Redis sem travamento prematuro de estoque.

### Entregáveis:
1. **Persistência Redis:**
   - `CartRepository` serializando o agregado `Cart` no Redis com TTL de 7 dias.
2. **Pre-flight Check:**
   - Consulta ao `CatalogQueryService` e `InventoryQueryService` para validação de disponibilidade antes do checkout.
3. **Endpoints REST:**
   - `POST /api/v1/cart/items`, `PUT /api/v1/cart/items/{sku}`, `DELETE /api/v1/cart/items/{sku}`, `GET /api/v1/cart`.

- **DoD:** Teste de expiração de TTL e checagem de indisponibilidade com Redis Testcontainers.

---

## Fase 6: Promoções & Frete (`promotions`, `shipping`)
- **Objetivo:** Validação de regras comerciais antes do checkout.

### Entregáveis:
1. **Promoções (`promotions`):**
   - Agregado `Coupon` (desconto percentual vs fixo, limite de uso, validade, valor mínimo).
   - `PromotionService` calculando o desconto aplicável para a ordem.
2. **Frete (`shipping`):**
   - SPI de cálculo de frete por CEP, peso e dimensões (com implementação Mock e Correios/Transportadora).

- **DoD:** Testes de regras de cupom expirado, limite de uso excedido e cálculo de frete por faixa de CEP.

---

## Fase 7: Pedidos & Checkout Transacional (`orders`)
- **Objetivo:** Orquestração transacional de checkout, snapshot contábil (`INV-004`) e máquina de estados (`INV-005`).

### Entregáveis:
1. **Modelagem de Snapshot (`INV-004`):**
   - `Order` e `OrderItem` sem chave estrangeira para `Product`.
   - Rateio de cupom pro-rata via `Money.distribute()` gravado em cada `OrderItemSnapshot`.
2. **Máquina de Estados:**
   - `OrderStatus`: `PAYMENT_PENDING` $\to$ `PAID` $\to$ `PREPARING` $\to$ `SHIPPED` $\to$ `DELIVERED` / `CANCELLED`.
3. **Transação 1 (Checkout Local ACID):**
   - Ingestão do carrinho $\to$ Reserva de estoque no `inventory` $\to$ Snapshot $\to$ Criação da Ordem em `PAYMENT_PENDING` $\to$ Publicação de `OrderCreatedEvent` no Outbox do Spring Modulith.
4. **Reconciliador de Pedidos Expirados:**
   - Job `@Scheduled` buscando ordens `PAYMENT_PENDING` com mais de 15 minutos e disparando cancelamento e liberação de estoque.

- **DoD:** Teste garantindo que alterações posteriores de preço no catálogo não afetam pedidos antigos, e que a soma dos snapshots fecha com precisão de 0 centavos.

---

## Fase 8: Pagamentos, Webhooks & SPI (`payments`)
- **Objetivo:** Processamento de pagamento, ingestão idempotente de webhooks (`INV-003`) e garantia de pagamento único (`INV-001`).

### Entregáveis:
1. **SPI de Pagamentos:**
   - Interface `PaymentProvider` com implementação de `FakePaymentProvider` para testes e `MercadoPagoPaymentProvider` / `StripePaymentProvider`.
2. **Ingestão de Webhooks com Invariante de Payload:**
   - Tabela `payment_webhook_events` (`provider`, `external_event_id`, `payload_hash`).
   - Validação estrita: mesmo hash $\to$ `200 OK`; hash diferente $\to$ `422 Unprocessable Entity`.
3. **Transação 2 (Confirmação de Pagamento - `INV-001`):**
   - SQL Condicional: `UPDATE orders SET status = 'PAID' WHERE id = :id AND status = 'PAYMENT_PENDING'`.
   - Transição de reserva para `FULFILLED` no `inventory`.
   - Publicação de `OrderPaidEvent` no Outbox.
4. **Reconciliador Ativo:**
   - Job ativo consultando pagamentos pendentes no gateway há mais de 10 minutos.

- **DoD (Milestone Test):** 10 webhooks simultâneos para o mesmo pedido resultando em exatamente 1 transição para `PAID` e 1 evento de Outbox.

---

## Fase 9: Verificação Estrutural Modulith & Hardening
- **Objetivo:** Auditoria arquitetural final, documentação e sincronização com PactX.

### Entregáveis:
1. **Verificação de Módulos (Spring Modulith):**
   - Teste automatizado `ApplicationModules.of(EcommerceApplication.class).verify()` garantindo zero violações de fronteira (`INV-006`).
   - Geração automática de diagramas de arquitetura (C4 / PlantUML via Spring Modulith).
2. **Documentação OpenAPI / Swagger:**
   - Exposição interativa dos endpoints e schemas em `/swagger-ui.html`.
3. **Fechamento no PactX:**
   - Execução de `pactx doctor --fix`.
   - Registro de todas as ADRs das decisões tomadas ao longo das fases.

- **DoD:** Suíte completa de testes passando em CI (100% verde) e documentação de arquitetura sincronizada.

---

### Próximo Passo Imediato

Estamos prontos para executar a **Fase 0 & Fase 1**:
1. Criar a estrutura do projeto e o `pom.xml` canônico com Java 21, Spring Boot 3.4 e Spring Modulith.
2. Escrever o módulo `common` (`Money` com `distribute()`, `AuthenticatedUser`, `IdempotencyEngine` e RFC 7807).

Podemos gerar o `pom.xml` e o código da **Fase 0/1** agora?