package com.ecargohub.backend.kafka;

import com.ecargohub.backend.dto.command.VehicleCommandDto;
import com.ecargohub.backend.dto.route.RouteResponseDto;
import com.ecargohub.backend.service.IdempotencyService;
import com.ecargohub.backend.service.SimulationRegistry;
import com.ecargohub.backend.service.geo.GraphHopperService;
import tools.jackson.databind.json.JsonMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class KafkaConsumerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerService.class);

    // Trayecto Cox → Murcia
    private static final double ORIGIN_LAT = 38.1408;
    private static final double ORIGIN_LON = -0.8844;
    private static final double DEST_LAT = 37.9922;
    private static final double DEST_LON = -1.1307;

    private static final int TOTAL_STEPS_FALLBACK = 20;
    private static final long STEP_DELAY_MS = 1000;

    // Rango de velocidad "realista" para el camión
    private static final double SPEED_MIN_KMH = 65.0;
    private static final double SPEED_MAX_KMH = 95.0;

    private final GraphHopperService graphHopperService;
    private final SimpMessagingTemplate messagingTemplate;
    private final IdempotencyService idempotencyService;
    private final JsonMapper objectMapper;
    private final ExecutorService simulationExecutor;
    private final SimulationRegistry simulationRegistry; // 👈 añadir al constructor

    public KafkaConsumerService(GraphHopperService graphHopperService, SimpMessagingTemplate messagingTemplate,
            IdempotencyService idempotencyService, SimulationRegistry simulationRegistry) {
        this.graphHopperService = graphHopperService;
        this.messagingTemplate = messagingTemplate;
        this.idempotencyService = idempotencyService;
        this.objectMapper = JsonMapper.builder().build();
        this.simulationExecutor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "simulation-worker");
            t.setDaemon(true);
            return t;
        });
        this.simulationRegistry = simulationRegistry;
    }

    @KafkaListener(topics = "vehicle-commands", groupId = "e-cargo-hub-consumers", containerFactory = "stringKafkaListenerContainerFactory")
    public void listenVehicleCommands(String messageString) {
        log.info("📥 ¡EVENTO DETECTADO EN KAFKA!: {}", messageString);

        try {
            VehicleCommandDto command = objectMapper.readValue(messageString, VehicleCommandDto.class);

            // 🔒 IDEMPOTENCIA
            if (!idempotencyService.tryMarkAsProcessed(command.commandId())) {
                return;
            }

            log.info("📦 Mapeado a DTO -> Comando: {}, Vehículo: {}, commandId: {}", command.command(),
                    command.vehicleId(), command.commandId());

            if (command.command() != null && "START".equalsIgnoreCase(command.command().name())) {
                RouteResponseDto route = buildRoute();

                final Long vehicleId = command.vehicleId();
                final RouteResponseDto routeFinal = route;
                final Future<?>[] holder = new Future<?>[1];

                holder[0] = simulationExecutor.submit(() -> {
                    try {
                        simulateVehicleMovement(vehicleId, routeFinal);
                    } finally {
                        simulationRegistry.unregister(vehicleId, holder[0]);
                    }
                });
                simulationRegistry.register(vehicleId, holder[0]);
            }

        } catch (Exception e) {
            log.error("❌ Error procesando el evento: {}", e.getMessage(), e);
            throw new RuntimeException("Error procesando comando de Kafka", e);
        }
    }

    /**
     * Intenta GraphHopper; si falla, cae a ruta lineal.
     */
    private RouteResponseDto buildRoute() {
        try {
            log.info("🛰️ Solicitando ruta a GraphHopper (Cox → Murcia)...");
            RouteResponseDto route = graphHopperService.calculateRoute(ORIGIN_LAT, ORIGIN_LON, DEST_LAT, DEST_LON);
            if (route != null && route.coordinates() != null && !route.coordinates().isEmpty()) {
                log.info("✅ Ruta GraphHopper: {} puntos", route.coordinates().size());
                return route;
            }
            log.warn("⚠️ GraphHopper devolvió ruta vacía, uso lineal.");
        } catch (Exception e) {
            log.warn("⚠️ GraphHopper falló ({}), uso lineal.", e.getMessage());
        }
        return buildLinearRoute();
    }

    private RouteResponseDto buildLinearRoute() {
        log.info("🛰️ Generando ruta lineal Cox ➔ Murcia (fallback offline)...");
        List<double[]> points = new ArrayList<>();
        for (int i = 0; i <= TOTAL_STEPS_FALLBACK; i++) {
            double pct = (double) i / TOTAL_STEPS_FALLBACK;
            points.add(new double[] { ORIGIN_LAT + (DEST_LAT - ORIGIN_LAT) * pct,
                    ORIGIN_LON + (DEST_LON - ORIGIN_LON) * pct });
        }
        return new RouteResponseDto(points, 27500.0, 1200.0);
    }

    private void simulateVehicleMovement(Long vehicleId, RouteResponseDto route) {
        List<double[]> coords = route.coordinates();
        int total = coords.size();

        log.info("🚀 Simulación iniciada para vehículo {}. Puntos: {}", vehicleId, total);

        // Emitimos estado inicial "EN_RUTA" en el primer punto
        for (int i = 0; i < total; i++) {
            double[] coord = coords.get(i);
            int progress = (int) Math.round(((double) (i + 1) / total) * 100);
            double speed = computeSpeed(i, total);

            Map<String, Object> telemetry = new HashMap<>();
            telemetry.put("vehicleId", vehicleId);
            telemetry.put("latitude", coord[0]);
            telemetry.put("longitude", coord[1]);
            telemetry.put("progress", progress);
            telemetry.put("speedKmh", speed);
            telemetry.put("status", "EN_RUTA");
            telemetry.put("timestamp", System.currentTimeMillis());

            log.info("📡 Punto {}/{} -> lat={}, lon={}, progress={}%, speed={} km/h", i + 1, total, coord[0], coord[1],
                    progress, String.format("%.1f", speed));

            messagingTemplate.convertAndSend("/topic/vehicle-status", (Object) telemetry);

            try {
                Thread.sleep(STEP_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Simulación interrumpida para vehículo {}", vehicleId);
                break;
            }
        }

        // Evento final de viaje completado
        Map<String, Object> finalStatus = new HashMap<>();
        finalStatus.put("vehicleId", vehicleId);
        finalStatus.put("latitude", DEST_LAT);
        finalStatus.put("longitude", DEST_LON);
        finalStatus.put("progress", 100);
        finalStatus.put("speedKmh", 0.0);
        finalStatus.put("status", "COMPLETADO");
        finalStatus.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/vehicle-status", (Object) finalStatus);
        log.info("🏁 Vehículo {} llegó a destino.", vehicleId);
    }

    /**
     * Velocidad variable de forma realista: - Arranca más lento (los primeros puntos acelerando). - Velocidad crucero
     * en medio. - Frena al llegar al destino. - Variación sinusoidal ligera para que no sea plano.
     */
    private double computeSpeed(int index, int total) {
        double pct = (double) index / (total - 1); // 0.0 → 1.0

        // Curva de "acelerar + crucero + frenar"
        double factor;
        if (pct < 0.15) {
            factor = pct / 0.15; // 0 → 1 en el primer 15%
        } else if (pct > 0.85) {
            factor = (1.0 - pct) / 0.15; // 1 → 0 en el último 15%
        } else {
            factor = 1.0; // crucero
        }

        // Variación ligera adicional para que no sea monótono
        double jitter = Math.sin(index * 0.7) * 0.05; // ±5%

        double speed = SPEED_MAX_KMH * (factor + jitter);
        speed = Math.max(SPEED_MIN_KMH * factor, Math.min(SPEED_MAX_KMH, speed));

        return Math.max(0.0, Math.round(speed * 10.0) / 10.0);
    }
}