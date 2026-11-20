package com.ecargohub.backend.config;

import com.ecargohub.backend.exception.ApiErrorResponseWriter;
import com.ecargohub.backend.service.ApiRateLimitService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiRateLimitFilterTest {

    private final ApiRateLimitService rateLimitService = mock(ApiRateLimitService.class);
    private final ApiRateLimitFilter filter = new ApiRateLimitFilter(rateLimitService,
            new ApiErrorResponseWriter(new ObjectMapper()), 2, 60);

    @AfterEach
    void clearSecurityContext() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void allowsRequestsWithinTheConfiguredLimit() throws Exception {
        when(rateLimitService.increment(anyString(), anyLong())).thenReturn(2L);
        AtomicBoolean chainCalled = new AtomicBoolean();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(apiRequest(), response, (request, result) -> chainCalled.set(true));

        assertTrue(chainCalled.get());
        assertEquals(200, response.getStatus());
    }

    @Test
    void rejectsRequestsAboveTheLimitWithRetryAfter() throws Exception {
        when(rateLimitService.increment(anyString(), anyLong())).thenReturn(3L);
        AtomicBoolean chainCalled = new AtomicBoolean();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(apiRequest(), response, (request, result) -> chainCalled.set(true));

        assertFalse(chainCalled.get());
        assertEquals(429, response.getStatus());
        assertTrue(Long.parseLong(response.getHeader("Retry-After")) > 0);
        assertTrue(response.getContentAsString().contains("\"error\":\"RATE_LIMITED\""));
    }

    @Test
    void returnsServiceUnavailableWhenRedisCannotApplyTheLimit() throws Exception {
        when(rateLimitService.increment(anyString(), anyLong()))
                .thenThrow(new DataAccessResourceFailureException("Redis unavailable"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(apiRequest(), response, (request, result) -> {
            throw new AssertionError("The request must not continue when the shared limiter is unavailable");
        });

        assertEquals(503, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"error\":\"RATE_LIMITER_UNAVAILABLE\""));
    }

    @Test
    void doesNotLimitNonApiRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v3/api-docs");
        request.setServletPath("/v3/api-docs");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> chainCalled.set(true));

        assertTrue(chainCalled.get());
        verify(rateLimitService, org.mockito.Mockito.never()).increment(anyString(), anyLong());
    }

    private MockHttpServletRequest apiRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/vehicles");
        request.setServletPath("/api/v1/vehicles");
        request.setRemoteAddr("127.0.0.1");
        return request;
    }
}
