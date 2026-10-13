package com.ecargohub.backend.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ControlTokenAuthenticationFilter extends OncePerRequestFilter {

    public static final String TOKEN_HEADER = "X-Control-Token";
    private static final int MIN_TOKEN_BYTES = 32;

    private final String expectedToken;

    public ControlTokenAuthenticationFilter(String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !ControlApiRequestMatcher.INSTANCE.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (expectedToken == null || expectedToken.getBytes(StandardCharsets.UTF_8).length < MIN_TOKEN_BYTES) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Control API is disabled: ECARGO_CONTROL_API_TOKEN must contain at least 32 UTF-8 bytes");
            return;
        }

        String suppliedToken = request.getHeader(TOKEN_HEADER);
        if (suppliedToken == null || !MessageDigest.isEqual(expectedToken.getBytes(StandardCharsets.UTF_8),
                suppliedToken.getBytes(StandardCharsets.UTF_8))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid control token");
            return;
        }

        var authentication = UsernamePasswordAuthenticationToken.authenticated("backend-control", null,
                AuthorityUtils.createAuthorityList("ROLE_CONTROL_SERVICE"));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
