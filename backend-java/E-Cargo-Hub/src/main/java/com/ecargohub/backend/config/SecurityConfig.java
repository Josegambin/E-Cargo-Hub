package com.ecargohub.backend.config;

import com.ecargohub.backend.exception.ApiErrorResponseWriter;
import com.ecargohub.backend.service.ApiRateLimitService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
            @Value("${app.security.keycloak-client-id:ecargo-dashboard}") String clientId) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> clientValidator = new JwtClaimValidator<>("azp",
                authorizedParty -> clientId.equals(authorizedParty));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, clientValidator));
        return decoder;
    }

    @Bean
    public ControlTokenAuthenticationFilter controlTokenAuthenticationFilter(
            @Value("${app.security.control-token:}") String controlToken, ApiErrorResponseWriter errorResponseWriter) {
        return new ControlTokenAuthenticationFilter(controlToken, errorResponseWriter);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            ControlTokenAuthenticationFilter controlTokenAuthenticationFilter,
            KeycloakJwtAuthenticationConverter jwtAuthenticationConverter,
            CorsConfigurationSource corsConfigurationSource, ApiRateLimitService rateLimitService,
            ApiErrorResponseWriter errorResponseWriter, @Value("${app.rate-limit.max-requests:100}") long maxRequests,
            @Value("${app.rate-limit.window-seconds:60}") long windowSeconds) throws Exception {
        AuthenticationEntryPoint authenticationEntryPoint = (request, response, exception) -> {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            errorResponseWriter.write(request, response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                    "Authentication is required or the access token is invalid", null);
        };
        AccessDeniedHandler accessDeniedHandler = (request, response, exception) -> errorResponseWriter.write(request,
                response, HttpStatus.FORBIDDEN, "FORBIDDEN", "User does not have permission to access this resource",
                null);

        http.csrf(csrf -> csrf.disable()).cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/", "/index.html", "/test-console/**").permitAll()
                        .requestMatchers("/api/v1/device-enrollments/claim", "/api/v1/device/state").permitAll()
                        .requestMatchers("/ws", "/ws/**", "/ws-native").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/v3/api-docs/**", "/swagger-ui/**",
                                "/swagger-ui.html")
                        .permitAll().requestMatchers(ControlApiRequestMatcher.INSTANCE)
                        .hasAnyRole("ADMIN", "CONTROL_SERVICE")
                        .requestMatchers(HttpMethod.GET, "/api/v1/vehicles/*/devices").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/**").hasAnyRole("ADMIN", "VIEWER")
                        .requestMatchers("/api/v1/**").hasRole("ADMIN").requestMatchers("/actuator/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .addFilterBefore(controlTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(
                        new ApiRateLimitFilter(rateLimitService, errorResponseWriter, maxRequests, windowSeconds),
                        ControlTokenAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:4200,http://127.0.0.1:4200,http://localhost:8081,http://127.0.0.1:8081,http://localhost,http://127.0.0.1}") String[] allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
