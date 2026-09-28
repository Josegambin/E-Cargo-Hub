package com.ecargohub.backend.kafka;

import com.ecargohub.backend.domain.enums.VehicleCommandTypeEnum;
import com.ecargohub.backend.domain.enums.VehicleStatusEnum;
import com.ecargohub.backend.dto.command.VehicleCommandDto;
import com.ecargohub.backend.dto.route.RouteResponseDto;
import com.ecargohub.backend.service.IdempotencyService;
import com.ecargohub.backend.service.SimulationRegistry;
import com.ecargohub.backend.service.SimulationRegistry.SimulationHandle;
import com.ecargohub.backend.service.VehicleStatusService;
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
    private final VehicleStatusService vehicleStatusService;

    public KafkaConsumerService(GraphHopperService graphHopperService, SimpMessagingTemplate messagingTemplate,
            IdempotencyService idempotencyService, SimulationRegistry simulationRegistry,
            VehicleStatusService vehicleStatusService) {
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
        this.vehicleStatusService = vehicleStatusService;
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

            VehicleCommandTypeEnum cmd = command.command();
            if (cmd == null) {
                log.warn("Comando nulo para vehículo {}", command.vehicleId());
                return;
            }

            Long vehicleId = command.vehicleId();

            switch (cmd) {
            case START -> handleStart(vehicleId);
            case PAUSE -> handlePause(vehicleId);
            case RESUME -> handleResume(vehicleId);
            case STOP -> handleStop(vehicleId);
            default -> log.warn("Comando desconocido: {}", cmd);
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

    private void simulateVehicleMovement(Long vehicleId, RouteResponseDto route, SimulationHandle handle) {
        List<double[]> coords = route.coordinates();
        int total = coords.size();

        log.info("🚀 Simulación iniciada para vehículo {}. Puntos: {}", vehicleId, total);

        int i = 0;
        while (i < total) {
            // ¿STOP solicitado?
            if (handle.isStopped()) {
                log.info("🛑 Vehículo {} detenido en punto {}/{}", vehicleId, i, total);
                handle.setState(VehicleStatusEnum.STOPPED);
                emitStopMessage(vehicleId, coords.get(Math.max(0, i - 1)));
                return;
            }

            // ¿PAUSA solicitada? Esperamos
            if (handle.isPaused()) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                continue;
            }

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

            messagingTemplate.convertAndSend("/topic/vehicle-status/" + vehicleId, (Object) telemetry);

            try {
                vehicleStatusService.updateStatus(vehicleId, coord[0], coord[1], progress, speed, "EN_RUTA");
            } catch (Exception e) {
                log.warn("No se pudo persistir posición: {}", e.getMessage());
            }

            try {
                Thread.sleep(STEP_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            i++;
        }

        // Llegó al final
        handle.setState(VehicleStatusEnum.COMPLETADO);
        Map<String, Object> finalStatus = new HashMap<>();
        finalStatus.put("vehicleId", vehicleId);
        finalStatus.put("latitude", DEST_LAT);
        finalStatus.put("longitude", DEST_LON);
        finalStatus.put("progress", 100);
        finalStatus.put("speedKmh", 0.0);
        finalStatus.put("status", "COMPLETADO");
        finalStatus.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/vehicle-status/" + vehicleId, (Object) finalStatus);

        try {
            vehicleStatusService.updateStatus(vehicleId, DEST_LAT, DEST_LON, 100, 0.0, "COMPLETADO");
        } catch (Exception e) {
            log.warn("No se pudo persistir estado final: {}", e.getMessage());
        }

        log.info("🏁 Vehículo {} llegó a destino.", vehicleId);
    }

    private void emitStopMessage(Long vehicleId, double[] lastCoord) {
        Map<String, Object> stopped = new HashMap<>();
        stopped.put("vehicleId", vehicleId);
        stopped.put("latitude", lastCoord[0]);
        stopped.put("longitude", lastCoord[1]);
        stopped.put("progress", null);
        stopped.put("speedKmh", 0.0);
        stopped.put("status", "STOPPED");
        stopped.put("timestamp", System.currentTimeMillis());
        messagingTemplate.convertAndSend("/topic/vehicle-status/" + vehicleId, (Object) stopped);

        try {
            vehicleStatusService.updateStatus(vehicleId, lastCoord[0], lastCoord[1], 0, 0.0, "STOPPED");
        } catch (Exception e) {
            log.warn("No se pudo persistir STOP: {}", e.getMessage());
        }
    }

    private void emitStatus(Long vehicleId, SimulationHandle h, String status, Double lat, Double lon, Integer progress,
            Double speed) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("vehicleId", vehicleId);
        msg.put("latitude", lat);
        msg.put("longitude", lon);
        msg.put("progress", progress);
        msg.put("speedKmh", speed != null ? speed : 0.0);
        msg.put("status", status);
        msg.put("timestamp", System.currentTimeMillis());
        messagingTemplate.convertAndSend("/topic/vehicle-status/" + vehicleId, (Object) msg);
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

    private void handleStart(Long vehicleId) {
        if (simulationRegistry.isRunning(vehicleId)) {
            log.warn("⚠️ Vehículo {} ya tiene una simulación activa. Ignorando START.", vehicleId);
            return;
        }

        RouteResponseDto route = buildRoute();
        final SimulationHandle[] holder = new SimulationHandle[1];

        Future<?> future = simulationExecutor.submit(() -> simulateVehicleMovement(vehicleId, route, holder[0]));

        holder[0] = simulationRegistry.register(vehicleId, future);

        // finally para desregistrar al terminar
        simulationExecutor.submit(() -> {
            try {
                future.get(); // espera a que termine
            } catch (Exception ignored) {
            }
            simulationRegistry.unregister(vehicleId, holder[0]);
        });
    }

    private void handlePause(Long vehicleId) {
        SimulationHandle h = simulationRegistry.get(vehicleId);
        if (h == null) {
            log.warn("⚠️ PAUSE sobre vehículo {} sin simulación activa", vehicleId);
            return;
        }
        if (h.isPaused()) {
            log.warn("⚠️ Vehículo {} ya está pausado", vehicleId);
            return;
        }
        h.pause();
        log.info("⏸️ Vehículo {} pausado", vehicleId);
        emitStatus(vehicleId, h, "PAUSADO", null, null, null, null);
    }

    private void handleResume(Long vehicleId) {
        SimulationHandle h = simulationRegistry.get(vehicleId);
        if (h == null) {
            log.warn("⚠️ RESUME sobre vehículo {} sin simulación activa", vehicleId);
            return;
        }
        if (!h.isPaused()) {
            log.warn("⚠️ Vehículo {} no está pausado", vehicleId);
            return;
        }
        h.resume();
        log.info("▶️ Vehículo {} reanudado", vehicleId);
        emitStatus(vehicleId, h, "EN_RUTA", null, null, null, null);
    }

    private void handleStop(Long vehicleId) {
        SimulationHandle h = simulationRegistry.get(vehicleId);
        if (h == null) {
            log.warn("⚠️ STOP sobre vehículo {} sin simulación activa", vehicleId);
            return;
        }
        h.requestStop();
        log.info("🛑 Vehículo {} detenido por comando", vehicleId);
    }
}