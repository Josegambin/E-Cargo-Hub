package com.ecargohub.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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
    public ControlTokenAuthenticationFilter controlTokenAuthenticationFilter(
            @Value("${app.security.control-token:}") String controlToken) {
        return new ControlTokenAuthenticationFilter(controlToken);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            ControlTokenAuthenticationFilter controlTokenAuthenticationFilter) throws Exception {
        http.csrf(csrf -> csrf.disable()) // Deshabilita CSRF para pruebas locales
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Aplica CORS abierto
                .authorizeHttpRequests(auth -> auth.requestMatchers(ControlApiRequestMatcher.INSTANCE)
                        .hasRole("CONTROL_SERVICE").requestMatchers("/ws/**").permitAll().anyRequest().permitAll())
                .addFilterBefore(controlTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*")); // Permite llamadas desde cualquier archivo local HTML
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
