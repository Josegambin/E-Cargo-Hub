package com.ecargohub.backend.client;

import com.ecargohub.backend.dto.geo.GeoLineStringDto;
import com.ecargohub.backend.dto.geo.GeoPointDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class GraphHopperClient {

    private final RestClient restClient;

    @Value("${graphhopper.api.key}")
    private String apiKey;

    @Value("${graphhopper.api.url:https://graphhopper.com/api/1}")
    private String baseUrl;

    public GraphHopperClient() {
        this.restClient = RestClient.builder().build();
    }

    /**
     * Calcula una ruta entre origen y destino usando la Routing API de GraphHopper.
     *
     * @return GeoLineString con la geometría, distancia (m) y duración (s)
     */
    public GraphHopperRouteResult calculateRoute(GeoPointDto origin, GeoPointDto destination) {
        // GraphHopper espera point=lat,lon
        String points = String.format("%f,%f&point=%f,%f",
                origin.latitude(), origin.longitude(),
                destination.latitude(), destination.longitude());

        Map<String, Object> response = restClient.get()
                .uri(baseUrl + "/route?key=" + apiKey
                        + "&profile=car"
                        + "&point=" + points
                        + "&points_encoded=false"
                        + "&instructions=false")
                .retrieve()
                .body(Map.class);

        if (response == null || !response.containsKey("paths")) {
            throw new IllegalStateException("GraphHopper no devolvió rutas");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> paths = (List<Map<String, Object>>) response.get("paths");
        if (paths.isEmpty()) {
            throw new IllegalStateException("GraphHopper devolvió lista de rutas vacía");
        }

        Map<String, Object> path = paths.get(0);

        Double distance = ((Number) path.get("distance")).doubleValue();       // metros
        Long time = ((Number) path.get("time")).longValue();                   // milisegundos

        @SuppressWarnings("unchecked")
        Map<String, Object> pointsMap = (Map<String, Object>) path.get("points");
        @SuppressWarnings("unchecked")
        List<List<Double>> coordinates = (List<List<Double>>) pointsMap.get("coordinates");

        GeoLineStringDto geometry = GeoLineStringDto.builder()
                .type("LineString")
                .coordinates(coordinates)
                .build();

        return new GraphHopperRouteResult(
                distance,
                time / 1000,   // segundos
                geometry
        );
    }

    /**
     * Resultado del cálculo de ruta.
     */
    public record GraphHopperRouteResult(
            Double distanceMeters,
            Long durationSeconds,
            GeoLineStringDto geometry
    ) {}
}