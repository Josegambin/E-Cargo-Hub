package com.ecargohub.backend.config;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlTokenAuthenticationFilterTest {

    private static final String TOKEN = "a-secret-control-token-with-at-least-32-bytes";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsMissingTokenForVehicleCommands() throws ServletException, IOException {
        MockHttpServletResponse response = executeFilter(new ControlTokenAuthenticationFilter(TOKEN),
                controlRequest("/api/v1/vehicles/1/commands", null));

        assertEquals(401, response.getStatus());
    }

    @Test
    void rejectsIncorrectTokenForFleetCommands() throws ServletException, IOException {
        MockHttpServletResponse response = executeFilter(new ControlTokenAuthenticationFilter(TOKEN),
                controlRequest("/api/v1/fleet/commands", "wrong-token"));

        assertEquals(401, response.getStatus());
    }

    @Test
    void rejectsControlCommandsWhenTokenIsNotConfigured() throws ServletException, IOException {
        MockHttpServletResponse response = executeFilter(new ControlTokenAuthenticationFilter(""),
                controlRequest("/api/v1/vehicles/1/aaos/simulated-commands", TOKEN));

        assertEquals(503, response.getStatus());
    }

    @Test
    void rejectsControlCommandsWhenConfiguredTokenIsTooShort() throws ServletException, IOException {
        MockHttpServletResponse response = executeFilter(new ControlTokenAuthenticationFilter("short-token"),
                controlRequest("/api/v1/vehicles/1/commands", "short-token"));

        assertEquals(503, response.getStatus());
    }

    @Test
    void acceptsCorrectTokenForSimulationControl() throws ServletException, IOException {
        MockHttpServletRequest request = controlRequest("/api/v1/simulations/2/start", TOKEN);
        MockFilterChain chain = new MockFilterChain();

        new ControlTokenAuthenticationFilter(TOKEN).doFilter(request, new MockHttpServletResponse(), chain);

        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_CONTROL_SERVICE")));
    }

    @Test
    void doesNotRequireTokenForVehicleCommandHistory() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setServletPath("/api/v1/vehicles/1/commands");
        MockFilterChain chain = new MockFilterChain();

        new ControlTokenAuthenticationFilter(TOKEN).doFilter(request, new MockHttpServletResponse(), chain);

        assertTrue(chain.getRequest() != null);
    }

    private MockHttpServletResponse executeFilter(ControlTokenAuthenticationFilter filter,
            MockHttpServletRequest request) throws ServletException, IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private MockHttpServletRequest controlRequest(String path, String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setServletPath(path);
        if (token != null) {
            request.addHeader(ControlTokenAuthenticationFilter.TOKEN_HEADER, token);
        }
        return request;
    }
}
