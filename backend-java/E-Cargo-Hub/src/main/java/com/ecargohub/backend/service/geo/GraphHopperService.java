package com.ecargohub.backend.service.geo;

import com.ecargohub.backend.dto.route.RouteResponseDto;

import tools.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Service
public class GraphHopperService {

    private static final Logger log = LoggerFactory.getLogger(GraphHopperService.class);
    private final RestClient restClient;

    @Value("${graphhopper.api.key}")
    private String apiKey;

    // Inyectamos la URL base definida en tu yml (https://graphhopper.com)
    public GraphHopperService(@Value("${graphhopper.api.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    /**
     * Conecta con la API de GraphHopper para obtener los puntos detallados de una ruta.
     */
    public RouteResponseDto calculateRoute(double startLat, double startLon, double endLat, double endLon) {
        try {
            log.info("Solicitando ruta a GraphHopper: [{}, {}] -> [{}, {}]", startLat, startLon, endLat, endLon);

            // Realiza la petición GET estructurando los Query Parameters requeridos por GraphHopper
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/route").queryParam("point", startLat + "," + startLon)
                            .queryParam("point", endLat + "," + endLon).queryParam("profile", "car") // Perfil de
                                                                                                     // conducción por
                                                                                                     // carretera
                            .queryParam("points_encoded", "false") // Crucial: desactiva la codificación para recibir un
                                                                   // JSON limpio con arrays de coordenadas
                            .queryParam("key", apiKey).build())
                    .retrieve().body(JsonNode.class);

            if (response != null && response.has("paths")) {
                JsonNode path = response.get("paths").get(0);
                double distance = path.get("distance").asDouble();
                double time = path.get("time").asDouble() / 1000.0; // Convierte milisegundos de la API a segundos

                List<double[]> coordinates = new ArrayList<>();
                JsonNode points = path.get("points").get("coordinates");

                for (JsonNode point : points) {
                    // GraphHopper responde por defecto en formato [longitud, latitud].
                    // Lo invertimos a [latitud, longitud] que es el estándar usado por Leaflet / Google Maps en el
                    // Frontend.
                    coordinates.add(new double[] { point.get(1).asDouble(), point.get(0).asDouble() });
                }

                log.info("Ruta procesada con éxito. Puntos geográficos: {}, Distancia: {}m", coordinates.size(),
                        distance);
                return new RouteResponseDto(coordinates, distance, time);
            }
        } catch (Exception e) {
            log.error("Error al consultar o parsear la ruta en GraphHopper API: {}", e.getMessage(), e);
        }

        // Fallback seguro: Si la API falla, devolvemos una línea recta entre origen y destino para evitar que la
        // aplicación se caiga
        log.warn("Usando ruta de contingencia en línea recta debido a un fallo en la API externa.");
        return new RouteResponseDto(List.of(new double[] { startLat, startLon }, new double[] { endLat, endLon }), 0.0,
                0.0);
    }
}
