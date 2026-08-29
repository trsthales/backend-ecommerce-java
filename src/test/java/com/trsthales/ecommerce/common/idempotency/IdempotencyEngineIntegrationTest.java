package com.trsthales.ecommerce.common.idempotency;

import com.trsthales.ecommerce.AbstractIntegrationTest;
import com.trsthales.ecommerce.common.exception.IdempotencyConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Idempotency Engine Integration Tests (INV-003)")
class IdempotencyEngineIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private IdempotencyKeyRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("Should execute request and return cached response on replay")
    void shouldExecuteAndReturnCachedResponse() {
        String key = "order-create-1001";
        String payloadHash = "sha256:abc123payload";

        // 1. First attempt -> should allow execution
        Optional<IdempotentResponse> firstAttempt = idempotencyService.startOrGet(key, payloadHash);
        assertThat(firstAttempt).isEmpty();

        // 2. Complete execution
        idempotencyService.complete(key, 201, "{\"orderId\":\"ord-1001\",\"status\":\"CREATED\"}");

        // 3. Duplicate attempt with same key and hash -> should return cached response
        Optional<IdempotentResponse> secondAttempt = idempotencyService.startOrGet(key, payloadHash);
        assertThat(secondAttempt).isPresent();
        assertThat(secondAttempt.get().statusCode()).isEqualTo(201);
        assertThat(secondAttempt.get().body()).isEqualTo("{\"orderId\":\"ord-1001\",\"status\":\"CREATED\"}");
    }

    @Test
    @DisplayName("Should reject concurrent in-flight requests with IdempotencyConflictException")
    void shouldRejectConcurrentInFlightRequests() {
        String key = "order-create-1002";
        String payloadHash = "sha256:concurrent123";

        // First thread acquires lock
        Optional<IdempotentResponse> lock = idempotencyService.startOrGet(key, payloadHash, Duration.ofMinutes(2));
        assertThat(lock).isEmpty();

        // Second concurrent thread tries before completion
        assertThatThrownBy(() -> idempotencyService.startOrGet(key, payloadHash))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessageContaining("Requisição concorrente em processamento");
    }

    @Test
    @DisplayName("Should reject request with payload mismatch for same key")
    void shouldRejectPayloadMismatch() {
        String key = "order-create-1003";
        String originalHash = "sha256:hash-original";
        String alteredHash = "sha256:hash-altered";

        idempotencyService.startOrGet(key, originalHash);
        idempotencyService.complete(key, 200, "{\"status\":\"OK\"}");

        assertThatThrownBy(() -> idempotencyService.startOrGet(key, alteredHash))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessageContaining("Divergência de payload");
    }

    @Test
    @DisplayName("Should recover after crash when lease lock expires")
    void shouldRecoverAfterCrashWhenLockExpires() {
        String key = "order-create-1004";
        String payloadHash = "sha256:crash123";

        // Simulate a crashed server by inserting a key locked in the past
        IdempotencyKeyEntity crashedKey = IdempotencyKeyEntity.builder()
                .key(key)
                .payloadHash(payloadHash)
                .status(IdempotencyStatus.PROCESSING)
                .createdAt(Instant.now().minusSeconds(180))
                .lockedUntil(Instant.now().minusSeconds(60)) // Lease already expired
                .build();
        repository.save(crashedKey);

        // Crash recovery: startOrGet should detect expired lease and allow re-execution
        Optional<IdempotentResponse> recoveryAttempt = idempotencyService.startOrGet(key, payloadHash);
        assertThat(recoveryAttempt).isEmpty();

        // Complete normally after recovery
        idempotencyService.complete(key, 201, "{\"recovered\":true}");

        Optional<IdempotentResponse> postRecovery = idempotencyService.startOrGet(key, payloadHash);
        assertThat(postRecovery).isPresent();
        assertThat(postRecovery.get().statusCode()).isEqualTo(201);
        assertThat(postRecovery.get().body()).isEqualTo("{\"recovered\":true}");
    }
}
