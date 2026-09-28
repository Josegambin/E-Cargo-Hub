package com.ecargohub.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private static final String KEY_PREFIX = "e-cargo-hub:processed-command:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redis;

    public IdempotencyService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * Intenta marcar un commandId como procesado.
     *
     * @return true si es la PRIMERA vez (debe procesarse), false si ya estaba (descartar).
     */
    public boolean tryMarkAsProcessed(UUID commandId) {
        if (commandId == null) {
            log.warn("commandId nulo, no se puede aplicar idempotencia");
            return true; // permitimos procesar para no bloquear el flujo
        }

        String key = KEY_PREFIX + commandId;
        Boolean wasSet = redis.opsForValue().setIfAbsent(key, "1", TTL);

        if (Boolean.TRUE.equals(wasSet)) {
            return true; // primera vez
        }

        log.warn("🔁 Comando duplicado detectado: {} — descartado", commandId);
        return false;
    }

    /**
     * Para tests o para forzar reprocesamiento.
     */
    public void clear(UUID commandId) {
        redis.delete(KEY_PREFIX + commandId);
    }
}