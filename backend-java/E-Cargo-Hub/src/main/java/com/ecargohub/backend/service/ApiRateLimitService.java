package com.ecargohub.backend.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApiRateLimitService {

    private static final RedisScript<Long> INCREMENT_WITH_EXPIRY = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return count
            """, Long.class);

    private final StringRedisTemplate redis;

    public ApiRateLimitService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public Long increment(String key, long ttlSeconds) {
        return redis.execute(INCREMENT_WITH_EXPIRY, List.of(key), Long.toString(ttlSeconds));
    }
}
