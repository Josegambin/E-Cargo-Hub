package com.ecargohub.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.List;
import java.util.regex.Pattern;

public final class ControlApiRequestMatcher implements RequestMatcher {

    public static final ControlApiRequestMatcher INSTANCE = new ControlApiRequestMatcher();

    private static final List<Pattern> CONTROL_PATHS = List.of(Pattern.compile("^/api/v1/vehicles/[^/]+/commands$"),
            Pattern.compile("^/api/v1/vehicles/[^/]+/aaos/simulated-commands$"),
            Pattern.compile("^/api/v1/vehicles/[^/]+/device-enrollments$"), Pattern.compile("^/api/v1/fleet/commands$"),
            Pattern.compile("^/api/v1/simulations/[^/]+/(start|pause|resume|stop)$"));

    private ControlApiRequestMatcher() {
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        String path = request.getServletPath();
        return CONTROL_PATHS.stream().anyMatch(pattern -> pattern.matcher(path).matches());
    }
}
