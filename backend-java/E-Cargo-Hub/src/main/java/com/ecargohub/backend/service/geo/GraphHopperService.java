package com.ecargohub.backend.service.geo;

import com.ecargohub.backend.dto.route.RouteResponseDto;
import tools.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GraphHopperService {

    private static final Logger log = LoggerFactory.getLogger(GraphHopperService.class);

    // Caché LRU simple: máximo 100 rutas distintas, la más antigua se descarta
    private static final int CACHE_MAX = 100;
    private final Map<String, RouteResponseDto> routeCache = Collections
            .synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, RouteResponseDto> eldest) {
                    boolean remove = size() > CACHE_MAX;
                    if (remove) {
                        log.debug("🗑️ Caché llena, descartando ruta: {}", eldest.getKey());
                    }
                    return remove;
                }
            });

    private final RestClient restClient;

    @Value("${graphhopper.api.key}")
    private String apiKey;

    public GraphHopperService(@Value("${graphhopper.api.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    /**
     * Devuelve la ruta entre dos puntos. 1. Si está en caché, la sirve al instante (sin llamar a la API). 2. Si no,
     * llama a GraphHopper. 3. Si GraphHopper falla, aplica fallback lineal y lo cachea también.
     */
    public RouteResponseDto calculateRoute(double startLat, double startLon, double endLat, double endLon) {

        String cacheKey = cacheKey(startLat, startLon, endLat, endLon);

        // 1. Intentar desde caché
        RouteResponseDto cached = routeCache.get(cacheKey);
        if (cached != null) {
            log.info("📦 Ruta servida desde caché: {} ({} puntos)", cacheKey, cached.coordinates().size());
            return cached;
        }

        // 2. Llamar a GraphHopper
        try {
            log.info("🛰️ Solicitando ruta a GraphHopper: [{}, {}] -> [{}, {}]", startLat, startLon, endLat, endLon);

            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/route").queryParam("point", startLat + "," + startLon)
                            .queryParam("point", endLat + "," + endLon).queryParam("profile", "car")
                            .queryParam("points_encoded", "false").queryParam("key", apiKey).build())
                    .retrieve().body(JsonNode.class);

            if (response != null && response.has("paths")) {
                JsonNode path = response.get("paths").get(0);
                double distance = path.get("distance").asDouble();
                double time = path.get("time").asDouble() / 1000.0;

                List<double[]> coordinates = new ArrayList<>();
                JsonNode points = path.get("points").get("coordinates");
                for (JsonNode point : points) {
                    // GraphHopper responde [lon, lat]; lo invertimos a [lat, lon]
                    coordinates.add(new double[] { point.get(1).asDouble(), point.get(0).asDouble() });
                }

                RouteResponseDto route = new RouteResponseDto(coordinates, distance, time);

                // 3. Guardar en caché
                routeCache.put(cacheKey, route);
                log.info("✅ Ruta GraphHopper cacheada: {} ({} puntos, {}m)", cacheKey, coordinates.size(), distance);

                return route;
            }

            log.warn("⚠️ GraphHopper devolvió respuesta sin paths. Aplicando fallback lineal.");

        } catch (Exception e) {
            log.error("❌ Error en GraphHopper: {}. Aplicando fallback lineal.", e.getMessage());
        }

        // 4. Fallback lineal (también cacheado para no reintentar sin parar)
        RouteResponseDto fallback = buildLinearRoute(startLat, startLon, endLat, endLon);
        routeCache.put(cacheKey, fallback);
        log.warn("💾 Fallback lineal cacheado: {}", cacheKey);
        return fallback;
    }

    /**
     * Genera una ruta lineal entre origen y destino con 21 puntos.
     */
    private RouteResponseDto buildLinearRoute(double startLat, double startLon, double endLat, double endLon) {
        log.warn("🛰️ Generando ruta lineal de [{}, {}] a [{}, {}]", startLat, startLon, endLat, endLon);

        List<double[]> points = new ArrayList<>();
        int steps = 20;
        for (int i = 0; i <= steps; i++) {
            double pct = (double) i / steps;
            points.add(new double[] { startLat + (endLat - startLat) * pct, startLon + (endLon - startLon) * pct });
        }
        return new RouteResponseDto(points, 0.0, 0.0);
    }

    private String cacheKey(double a, double b, double c, double d) {
        return String.format("%.4f,%.4f->%.4f,%.4f", a, b, c, d);
    }

    /**
     * Limpia la caché (útil para tests o endpoints de administración).
     */
    public void clearCache() {
        int size = routeCache.size();
        routeCache.clear();
        log.info("🗑️ Caché de rutas limpiada ({} entradas eliminadas)", size);
    }

    /**
     * Número de rutas cacheadas actualmente.
     */
    public int getCacheSize() {
        return routeCache.size();
    }
}