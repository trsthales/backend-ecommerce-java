package com.trsthales.ecommerce.common.idempotency;

import com.trsthales.ecommerce.common.exception.IdempotencyConflictException;
import com.trsthales.ecommerce.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Service
public class IdempotencyService {

    public static final Duration DEFAULT_LOCK_DURATION = Duration.ofMinutes(2);

    private final IdempotencyKeyRepository repository;

    public IdempotencyService(IdempotencyKeyRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Optional<IdempotentResponse> startOrGet(String key, String payloadHash) {
        return startOrGet(key, payloadHash, DEFAULT_LOCK_DURATION);
    }

    @Transactional
    public Optional<IdempotentResponse> startOrGet(String key, String payloadHash, Duration lockDuration) {
        Objects.requireNonNull(key, "Chave de idempotência é obrigatória");
        Objects.requireNonNull(payloadHash, "Payload hash é obrigatório");

        Duration effectiveDuration = (lockDuration != null) ? lockDuration : DEFAULT_LOCK_DURATION;
        Instant now = Instant.now();

        Optional<IdempotencyKeyEntity> existingOpt = repository.findById(key);

        if (existingOpt.isEmpty()) {
            IdempotencyKeyEntity newKey = IdempotencyKeyEntity.builder()
                    .key(key)
                    .payloadHash(payloadHash)
                    .status(IdempotencyStatus.PROCESSING)
                    .createdAt(now)
                    .lockedUntil(now.plus(effectiveDuration))
                    .build();
            repository.save(newKey);
            return Optional.empty();
        }

        IdempotencyKeyEntity entity = existingOpt.get();

        if (!Objects.equals(entity.getPayloadHash(), payloadHash)) {
            throw new IdempotencyConflictException("Divergência de payload para a chave de idempotência: " + key);
        }

        if (entity.getStatus() == IdempotencyStatus.COMPLETED) {
            return Optional.of(new IdempotentResponse(
                    entity.getResponseCode() != null ? entity.getResponseCode() : 200,
                    entity.getResponseBody()
            ));
        }

        if (entity.getStatus() == IdempotencyStatus.PROCESSING) {
            if (entity.getLockedUntil().isAfter(now)) {
                throw new IdempotencyConflictException("Requisição concorrente em processamento para a chave: " + key);
            }

            // Crash recovery: o lease de execução expirou enquanto estava em PROCESSING
            entity.setLockedUntil(now.plus(effectiveDuration));
            repository.save(entity);
            return Optional.empty();
        }

        if (entity.getStatus() == IdempotencyStatus.FAILED) {
            // Re-tentativa permitida para operações com falha prévia
            entity.setStatus(IdempotencyStatus.PROCESSING);
            entity.setLockedUntil(now.plus(effectiveDuration));
            repository.save(entity);
            return Optional.empty();
        }

        return Optional.empty();
    }

    @Transactional
    public void complete(String key, int responseCode, String responseBody) {
        IdempotencyKeyEntity entity = repository.findById(key)
                .orElseThrow(() -> new ResourceNotFoundException("Chave de idempotência não encontrada: " + key));

        entity.setStatus(IdempotencyStatus.COMPLETED);
        entity.setResponseCode(responseCode);
        entity.setResponseBody(responseBody);
        repository.save(entity);
    }

    @Transactional
    public void fail(String key) {
        repository.findById(key).ifPresent(entity -> {
            entity.setStatus(IdempotencyStatus.FAILED);
            repository.save(entity);
        });
    }
}
