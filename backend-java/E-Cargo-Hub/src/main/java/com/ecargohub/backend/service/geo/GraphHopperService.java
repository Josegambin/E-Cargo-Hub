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
import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;

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
     * Uses the vehicle's configured maximum speed only where GraphHopper explicitly returns null.
     */
    public RouteResponseDto calculateRoute(double startLat, double startLon, double endLat, double endLon,
            double vehicleMaxSpeedKmh) {

        String cacheKey = cacheKey(startLat, startLon, endLat, endLon, vehicleMaxSpeedKmh);

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
                            .queryParam("points_encoded", "false").queryParam("instructions", "false")
                            .queryParam("details", "max_speed").queryParam("key", apiKey).build())
                    .retrieve().body(JsonNode.class);

            if (response != null && response.has("paths")) {
                JsonNode path = response.get("paths").get(0);
                double distance = path.get("distance").asDouble();
                double time = path.get("time").asDouble() / 1000.0;

                List<double[]> coordinates = new ArrayList<>();
                JsonNode points = path.get("points").get("coordinates");
                for (JsonNode point : points) {
                    coordinates.add(new double[] { point.get(1).asDouble(), point.get(0).asDouble() });
                }

                JsonNode speedDetails = path.path("details").path("max_speed");
                int fallbackSegments = countNullSpeedLimitSegments(speedDetails);
                List<Double> speedLimits = extractSpeedLimits(speedDetails, coordinates.size() - 1, vehicleMaxSpeedKmh);
                if (fallbackSegments > 0) {
                    log.warn("GraphHopper returned max_speed=null for {} route segments; using the vehicle maximum "
                            + "of {} km/h for those segments", fallbackSegments, vehicleMaxSpeedKmh);
                }
                RouteResponseDto route = new RouteResponseDto(coordinates, distance, time, speedLimits);

                routeCache.put(cacheKey, route);
                log.info("✅ Ruta GraphHopper cacheada: {} ({} puntos, {}m)", cacheKey, coordinates.size(), distance);

                return route;
            }

            throw new IllegalStateException("GraphHopper did not return a routed path");

        } catch (IllegalStateException e) {
            log.error("❌ Unable to calculate a route with per-road speed limits: {}", e.getMessage());
            throw new IllegalStateException("Route unavailable: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Unable to calculate a route with per-road speed limits: {}", e.getMessage());
            throw new IllegalStateException("Could not calculate route from GraphHopper: " + e.getMessage(), e);
        }
    }

    static List<Double> extractSpeedLimits(JsonNode details, int segmentCount) {
        return extractSpeedLimits(details, segmentCount, Double.NaN);
    }

    static List<Double> extractSpeedLimits(JsonNode details, int segmentCount, double vehicleMaxSpeedKmh) {
        if (segmentCount < 1 || !details.isArray() || details.isEmpty()) {
            throw new IllegalStateException("Route has no max_speed details");
        }

        Double[] limits = new Double[segmentCount];
        for (JsonNode detail : details) {
            if (!detail.isArray() || detail.size() < 3 || !detail.get(0).canConvertToInt()
                    || !detail.get(1).canConvertToInt()) {
                throw new IllegalStateException("Route contains an invalid max_speed detail: " + detail);
            }
            int from = detail.get(0).asInt();
            int to = detail.get(1).asInt();
            JsonNode rawLimit = detail.get(2);
            double limit;
            if (rawLimit.isNull()) {
                if (!Double.isFinite(vehicleMaxSpeedKmh) || vehicleMaxSpeedKmh <= 0) {
                    throw new IllegalStateException(
                            "Route contains a null max_speed and no valid vehicle maximum " + "speed fallback");
                }
                limit = vehicleMaxSpeedKmh;
            } else {
                limit = parseSpeedLimit(rawLimit);
            }
            if (!Double.isFinite(limit) || limit <= 0 || from < 0 || to > segmentCount || from >= to) {
                throw new IllegalStateException("Route contains an invalid max_speed range");
            }
            for (int segment = from; segment < to; segment++) {
                limits[segment] = limit;
            }
        }
        for (Double limit : limits) {
            if (limit == null) {
                throw new IllegalStateException("At least one road segment has no max_speed data");
            }
        }
        return List.of(limits);
    }

    private static int countNullSpeedLimitSegments(JsonNode details) {
        int segmentCount = 0;
        for (JsonNode detail : details) {
            if (detail.isArray() && detail.size() >= 3 && detail.get(2).isNull() && detail.get(0).canConvertToInt()
                    && detail.get(1).canConvertToInt()) {
                segmentCount += Math.max(0, detail.get(1).asInt() - detail.get(0).asInt());
            }
        }
        return segmentCount;
    }

    private static double parseSpeedLimit(JsonNode value) {
        if (value.isNumber()) {
            return value.asDouble();
        }
        if (value.isString()) {
            try {
                return Double.parseDouble(value.asString().trim());
            } catch (NumberFormatException e) {
                throw nonNumericSpeedLimit(value, e);
            }
        }
        throw nonNumericSpeedLimit(value, null);
    }

    private static IllegalStateException nonNumericSpeedLimit(JsonNode value, Exception cause) {
        String type = value == null ? "missing" : value.getNodeType().toString();
        String rawValue = value == null ? "<missing>" : value.toString();
        String message = "Route contains a non-numeric max_speed value (type=" + type + ", value=" + rawValue + ")";
        return cause == null ? new IllegalStateException(message) : new IllegalStateException(message, cause);
    }

    private String cacheKey(double a, double b, double c, double d, double vehicleMaxSpeedKmh) {
        return String.format("%.4f,%.4f->%.4f,%.4f|max=%s", a, b, c, d, Double.toString(vehicleMaxSpeedKmh));
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