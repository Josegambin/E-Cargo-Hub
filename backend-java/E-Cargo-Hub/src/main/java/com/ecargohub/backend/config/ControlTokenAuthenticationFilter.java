package com.ecargohub.backend.config;

import com.ecargohub.backend.exception.ApiErrorResponseWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ControlTokenAuthenticationFilter extends OncePerRequestFilter {

    public static final String TOKEN_HEADER = "X-Control-Token";
    private static final int MIN_TOKEN_BYTES = 32;

    private final String expectedToken;
    private final ApiErrorResponseWriter errorResponseWriter;

    public ControlTokenAuthenticationFilter(String expectedToken, ApiErrorResponseWriter errorResponseWriter) {
        this.expectedToken = expectedToken;
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        return !ControlApiRequestMatcher.INSTANCE.matches(request)
                || (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (expectedToken == null || expectedToken.getBytes(StandardCharsets.UTF_8).length < MIN_TOKEN_BYTES) {
            errorResponseWriter.write(request, response, HttpStatus.SERVICE_UNAVAILABLE, "CONTROL_API_DISABLED",
                    "The control API is not configured", null);
            return;
        }

        String suppliedToken = request.getHeader(TOKEN_HEADER);
        if (suppliedToken == null || !MessageDigest.isEqual(expectedToken.getBytes(StandardCharsets.UTF_8),
                suppliedToken.getBytes(StandardCharsets.UTF_8))) {
            errorResponseWriter.write(request, response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                    "Missing or invalid control token", null);
            return;
        }

        var authentication = UsernamePasswordAuthenticationToken.authenticated("backend-control", null,
                AuthorityUtils.createAuthorityList("ROLE_CONTROL_SERVICE"));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
