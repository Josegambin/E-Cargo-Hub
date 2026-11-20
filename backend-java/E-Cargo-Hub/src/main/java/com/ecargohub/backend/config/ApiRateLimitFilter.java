package com.ecargohub.backend.config;

import com.ecargohub.backend.exception.ApiErrorResponseWriter;
import com.ecargohub.backend.service.ApiRateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

public class ApiRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiRateLimitFilter.class);
    private static final String KEY_PREFIX = "e-cargo-hub:api-rate-limit:v1:";

    private final ApiRateLimitService rateLimitService;
    private final ApiErrorResponseWriter errorResponseWriter;
    private final long maxRequests;
    private final long windowSeconds;

    public ApiRateLimitFilter(ApiRateLimitService rateLimitService, ApiErrorResponseWriter errorResponseWriter,
            long maxRequests, long windowSeconds) {
        if (maxRequests < 1 || windowSeconds < 1) {
            throw new IllegalArgumentException("Rate limit and window must be positive");
        }
        this.rateLimitService = rateLimitService;
        this.errorResponseWriter = errorResponseWriter;
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith("/api/v1/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long nowSeconds = Instant.now().getEpochSecond();
        long windowIndex = Math.floorDiv(nowSeconds, windowSeconds);
        String redisKey = KEY_PREFIX + digest(rateLimitIdentity(request)) + ":" + windowIndex;

        Long requestCount;
        try {
            requestCount = rateLimitService.increment(redisKey, windowSeconds);
        } catch (DataAccessException ex) {
            log.error("API rate limiter is unavailable for {}", request.getServletPath(), ex);
            errorResponseWriter.write(request, response, HttpStatus.SERVICE_UNAVAILABLE, "RATE_LIMITER_UNAVAILABLE",
                    "The API rate limiter is temporarily unavailable", null);
            return;
        }
        if (requestCount == null) {
            log.error("API rate limiter returned no counter for {}", request.getServletPath());
            errorResponseWriter.write(request, response, HttpStatus.SERVICE_UNAVAILABLE, "RATE_LIMITER_UNAVAILABLE",
                    "The API rate limiter is temporarily unavailable", null);
            return;
        }

        if (requestCount > maxRequests) {
            long retryAfter = Math.max(1, (windowIndex + 1) * windowSeconds - nowSeconds);
            errorResponseWriter.write(request, response, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
                    "Too many API requests; try again after the rate limit window resets", retryAfter);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String rateLimitIdentity(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken) && authentication.getName() != null
                && !authentication.getName().isBlank()) {
            return "user:" + authentication.getName();
        }
        return "ip:" + (request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr());
    }

    private String digest(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

}
