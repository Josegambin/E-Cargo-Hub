package com.ecargohub.backend.client;

import com.ecargohub.backend.dto.geo.GeoLineStringDto;
import com.ecargohub.backend.dto.geo.GeoPointDto;
import com.ecargohub.backend.exception.ExternalServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class GraphHopperClient {

    private final RestClient restClient;
    private final String apiKey;

    public GraphHopperClient(@Value("${graphhopper.api.url:https://graphhopper.com/api/1}") String baseUrl,
            @Value("${graphhopper.api.key:}") String apiKey) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    /**
     * Calcula una ruta entre origen y destino usando la Routing API de GraphHopper.
     *
     * @return GeoLineString con la geometría, distancia (m) y duración (s)
     */
    public GraphHopperRouteResult calculateRoute(GeoPointDto origin, GeoPointDto destination) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ExternalServiceUnavailableException("GraphHopper API key is not configured");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> response;
        try {
            response = restClient.get().uri(uriBuilder -> uriBuilder.path("/route").queryParam("key", apiKey)
                    .queryParam("profile", "car")
                    .queryParam("point", String.format(Locale.ROOT, "%f,%f", origin.latitude(), origin.longitude()))
                    .queryParam("point",
                            String.format(Locale.ROOT, "%f,%f", destination.latitude(), destination.longitude()))
                    .queryParam("points_encoded", "false").queryParam("instructions", "false").build()).retrieve()
                    .body(Map.class);
        } catch (RestClientException ex) {
            throw new ExternalServiceUnavailableException("GraphHopper routing service is unavailable", ex);
        }

        if (response == null || !response.containsKey("paths")) {
            throw new IllegalStateException("GraphHopper no devolvió rutas");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> paths = (List<Map<String, Object>>) response.get("paths");
        if (paths.isEmpty()) {
            throw new IllegalStateException("GraphHopper devolvió lista de rutas vacía");
        }

        Map<String, Object> path = paths.get(0);

        Double distance = ((Number) path.get("distance")).doubleValue(); // metros
        Long time = ((Number) path.get("time")).longValue(); // milisegundos

        @SuppressWarnings("unchecked")
        Map<String, Object> pointsMap = (Map<String, Object>) path.get("points");
        @SuppressWarnings("unchecked")
        List<List<Double>> coordinates = (List<List<Double>>) pointsMap.get("coordinates");

        GeoLineStringDto geometry = GeoLineStringDto.builder().type("LineString").coordinates(coordinates).build();

        return new GraphHopperRouteResult(distance, time / 1000, // segundos
                geometry);
    }

    /**
     * Resultado del cálculo de ruta.
     */
    public record GraphHopperRouteResult(Double distanceMeters, Long durationSeconds, GeoLineStringDto geometry) {
    }
}