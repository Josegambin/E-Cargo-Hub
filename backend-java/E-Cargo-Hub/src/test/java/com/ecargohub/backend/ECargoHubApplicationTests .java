package com.ecargohub.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ecargohub.backend.config.VehicleCommandConstraintInitializer;
import com.ecargohub.backend.service.ApiRateLimitService;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:ecargohub;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@EmbeddedKafka(
    partitions = 1, 
    ports = 8085 // <- Le dice nativamente a KRaft que levante el broker en el puerto 8085
)
class ECargoHubApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VehicleCommandConstraintInitializer vehicleCommandConstraintInitializer;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private ApiRateLimitService apiRateLimitService;

    @BeforeEach
    void configureDashboardTokens() {
        when(jwtDecoder.decode("viewer-jwt")).thenReturn(dashboardToken("viewer-jwt", "VIEWER"));
        when(jwtDecoder.decode("admin-jwt")).thenReturn(dashboardToken("admin-jwt", "ADMIN"));
        when(apiRateLimitService.increment(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong())).thenReturn(1L);
    }

    @Test
    void contextLoads() {
    }

    @Test
    void dashboardApiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void unknownApiEndpointReturnsStandardNotFoundError() throws Exception {
        mockMvc.perform(get("/api/v1/not-a-real-endpoint").header("Authorization", "Bearer admin-jwt"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void testConsoleAndAssetsArePublic() throws Exception {
        mockMvc.perform(get("/test-console/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/test-console/index.html"));

        mockMvc.perform(get("/test-console/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Prueba el backend sin Angular")));

        mockMvc.perform(get("/test-console/app.js"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/test-console/styles.css"))
                .andExpect(status().isOk());
    }

    @Test
    void viewerCanReadButCannotCreateVehicles() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles").header("Authorization", "Bearer viewer-jwt"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/vehicles/1/devices")
                .header("Authorization", "Bearer viewer-jwt"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/vehicles")
                .header("Authorization", "Bearer viewer-jwt")
                .contentType(APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        mockMvc.perform(post("/api/v1/vehicles")
                .header("Authorization", "Bearer admin-jwt")
                .contentType(APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dashboardAdminCanReachControlApiWithoutSharedControlToken() throws Exception {
        mockMvc.perform(post("/api/v1/vehicles/999/commands")
                .header("Authorization", "Bearer admin-jwt")
                .contentType(APPLICATION_JSON)
                .content("{\"command\":\"START\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void vehicleStatusAndVhalStatusRejectUnknownVehicles() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles/999/status")
                .header("Authorization", "Bearer admin-jwt"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/vehicles/999/commands/vhal-status")
                .header("Authorization", "Bearer admin-jwt"))
                .andExpect(status().isNotFound());
    }

    private Jwt dashboardToken(String token, String role) {
        Instant now = Instant.now();
        return Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .subject("test-user")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("preferred_username", "test-user")
                .claim("realm_access", Map.of("roles", List.of(role)))
                .build();
    }
}