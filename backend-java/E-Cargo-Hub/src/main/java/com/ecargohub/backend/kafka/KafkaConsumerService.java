package com.ecargohub.backend.kafka;

import com.ecargohub.backend.domain.enums.VehicleCommandTypeEnum;
import com.ecargohub.backend.domain.enums.VehicleStatusEnum;
import com.ecargohub.backend.dto.command.VehicleCommandDto;
import com.ecargohub.backend.dto.route.RouteResponseDto;
import com.ecargohub.backend.service.FleetEventService;
import com.ecargohub.backend.service.IdempotencyService;
import com.ecargohub.backend.service.SimulationRegistry;
import com.ecargohub.backend.service.SimulationRegistry.SimulationHandle;
import com.ecargohub.backend.service.VehicleStatusService;
import com.ecargohub.backend.service.VehicleTelemetryService;
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

    private static final double DEFAULT_ORIGIN_LAT = 38.1408;
    private static final double DEFAULT_ORIGIN_LON = -0.8844;
    private static final double DEFAULT_DEST_LAT = 37.9922;
    private static final double DEFAULT_DEST_LON = -1.1307;

    private static final int TOTAL_STEPS_FALLBACK = 20;
    private static final long STEP_DELAY_MS = 1000;

    private static final double SPEED_MIN_KMH = 65.0;
    private static final double SPEED_MAX_KMH = 95.0;

    private final GraphHopperService graphHopperService;
    private final SimpMessagingTemplate messagingTemplate;
    private final IdempotencyService idempotencyService;
    private final JsonMapper objectMapper;
    private final ExecutorService simulationExecutor;
    private final SimulationRegistry simulationRegistry;
    private final VehicleStatusService vehicleStatusService;
    private final VehicleTelemetryService vehicleTelemetryService;
    private final FleetEventService fleetEventService;

    public KafkaConsumerService(GraphHopperService graphHopperService, SimpMessagingTemplate messagingTemplate,
            IdempotencyService idempotencyService, SimulationRegistry simulationRegistry,
            VehicleStatusService vehicleStatusService, VehicleTelemetryService vehicleTelemetryService,
            FleetEventService fleetEventService) {
        this.graphHopperService = graphHopperService;
        this.messagingTemplate = messagingTemplate;
        this.idempotencyService = idempotencyService;
        this.objectMapper = JsonMapper.builder().build();
        this.simulationExecutor = Executors.newFixedThreadPool(8, r -> {
            Thread t = new Thread(r, "simulation-worker");
            t.setDaemon(true);
            return t;
        });
        this.simulationRegistry = simulationRegistry;
        this.vehicleStatusService = vehicleStatusService;
        this.vehicleTelemetryService = vehicleTelemetryService;
        this.fleetEventService = fleetEventService;
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

            switch (cmd) {
            case START -> handleStart(command);
            case PAUSE -> handlePause(command.vehicleId());
            case RESUME -> handleResume(command.vehicleId());
            case STOP -> handleStop(command.vehicleId());
            default -> log.warn("Comando desconocido: {}", cmd);
            }

        } catch (Exception e) {
            log.error("❌ Error procesando el evento: {}", e.getMessage(), e);
            throw new RuntimeException("Error procesando comando de Kafka", e);
        }
    }

    // ==========================================
    // HANDLERS DE COMANDOS
    // ==========================================

    private void handleStart(VehicleCommandDto command) {
        Long vehicleId = command.vehicleId();

        if (simulationRegistry.isRunning(vehicleId)) {
            log.warn("⚠️ Vehículo {} ya tiene una simulación activa. Ignorando START.", vehicleId);
            return;
        }

        RouteResponseDto route = buildRoute(command);

        final SimulationHandle[] holder = new SimulationHandle[1];

        Future<?> future = simulationExecutor.submit(() -> simulateVehicleMovement(vehicleId, route, holder[0]));

        holder[0] = simulationRegistry.register(vehicleId, future);

        simulationExecutor.submit(() -> {
            try {
                future.get();
            } catch (Exception ignored) {
            }
            simulationRegistry.unregister(vehicleId, holder[0]);
        });

        // 🆕 Evento global de flota
        fleetEventService.vehicleStarted(vehicleId);
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
        emitStatus(vehicleId, "PAUSADO");
        fleetEventService.vehiclePaused(vehicleId); // 🆕
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
        emitStatus(vehicleId, "EN_RUTA");
        fleetEventService.vehicleResumed(vehicleId); // 🆕
    }

    private void handleStop(Long vehicleId) {
        SimulationHandle h = simulationRegistry.get(vehicleId);
        if (h == null) {
            log.warn("⚠️ STOP sobre vehículo {} sin simulación activa", vehicleId);
            return;
        }
        h.requestStop();
        log.info("🛑 Vehículo {} detenido por comando", vehicleId);
        fleetEventService.vehicleStopped(vehicleId); // 🆕
    }

    // ==========================================
    // RUTAS (parametrizadas con fallback)
    // ==========================================

    private RouteResponseDto buildRoute(VehicleCommandDto command) {
        double originLat = command.originLat() != null ? command.originLat() : DEFAULT_ORIGIN_LAT;
        double originLon = command.originLon() != null ? command.originLon() : DEFAULT_ORIGIN_LON;
        double destLat = command.destLat() != null ? command.destLat() : DEFAULT_DEST_LAT;
        double destLon = command.destLon() != null ? command.destLon() : DEFAULT_DEST_LON;

        log.info("🛰️ Ruta solicitada: ({}, {}) → ({}, {})", originLat, originLon, destLat, destLon);

        try {
            RouteResponseDto route = graphHopperService.calculateRoute(originLat, originLon, destLat, destLon);

            if (route != null && route.coordinates() != null && !route.coordinates().isEmpty()) {
                log.info("✅ Ruta obtenida: {} puntos", route.coordinates().size());
                return route;
            }
            log.warn("⚠️ Ruta vacía, uso fallback lineal.");
        } catch (Exception e) {
            log.warn("⚠️ GraphHopper falló ({}), uso fallback lineal.", e.getMessage());
        }

        return buildLinearRoute(originLat, originLon, destLat, destLon);
    }

    private RouteResponseDto buildLinearRoute(double fromLat, double fromLon, double toLat, double toLon) {
        log.info("🛰️ Generando ruta lineal ({}, {}) → ({}, {})", fromLat, fromLon, toLat, toLon);

        List<double[]> points = new ArrayList<>();
        for (int i = 0; i <= TOTAL_STEPS_FALLBACK; i++) {
            double pct = (double) i / TOTAL_STEPS_FALLBACK;
            points.add(new double[] { fromLat + (toLat - fromLat) * pct, fromLon + (toLon - fromLon) * pct });
        }
        return new RouteResponseDto(points, 0.0, 0.0);
    }

    // ==========================================
    // SIMULACIÓN DEL MOVIMIENTO
    // ==========================================

    private void simulateVehicleMovement(Long vehicleId, RouteResponseDto route, SimulationHandle handle) {
        List<double[]> coords = route.coordinates();
        int total = coords.size();

        double finalLat = coords.get(total - 1)[0];
        double finalLon = coords.get(total - 1)[1];

        log.info("🚀 Simulación iniciada para vehículo {}. Puntos: {}", vehicleId, total);

        int i = 0;
        while (i < total) {
            if (handle.isStopped()) {
                log.info("🛑 Vehículo {} detenido en punto {}/{}", vehicleId, i, total);
                handle.setState(VehicleStatusEnum.STOPPED);
                emitStopMessage(vehicleId, coords.get(Math.max(0, i - 1)));
                return;
            }

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
                log.warn("No se pudo persistir última posición: {}", e.getMessage());
            }

            try {
                vehicleTelemetryService.savePoint(vehicleId, coord[0], coord[1], progress, speed, "EN_RUTA");
            } catch (Exception e) {
                log.warn("No se pudo persistir punto de telemetría: {}", e.getMessage());
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
        finalStatus.put("latitude", finalLat);
        finalStatus.put("longitude", finalLon);
        finalStatus.put("progress", 100);
        finalStatus.put("speedKmh", 0.0);
        finalStatus.put("status", "COMPLETADO");
        finalStatus.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/vehicle-status/" + vehicleId, (Object) finalStatus);

        try {
            vehicleStatusService.updateStatus(vehicleId, finalLat, finalLon, 100, 0.0, "COMPLETADO");
        } catch (Exception e) {
            log.warn("No se pudo persistir estado final: {}", e.getMessage());
        }

        try {
            vehicleTelemetryService.savePoint(vehicleId, finalLat, finalLon, 100, 0.0, "COMPLETADO");
        } catch (Exception e) {
            log.warn("No se pudo persistir punto final: {}", e.getMessage());
        }

        log.info("🏁 Vehículo {} llegó a destino ({}, {})", vehicleId, finalLat, finalLon);
        fleetEventService.vehicleCompleted(vehicleId); // 🆕
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

        try {
            vehicleTelemetryService.savePoint(vehicleId, lastCoord[0], lastCoord[1], 0, 0.0, "STOPPED");
        } catch (Exception e) {
            log.warn("No se pudo persistir STOP en telemetría: {}", e.getMessage());
        }
    }

    private void emitStatus(Long vehicleId, String status) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("vehicleId", vehicleId);
        msg.put("latitude", null);
        msg.put("longitude", null);
        msg.put("progress", null);
        msg.put("speedKmh", 0.0);
        msg.put("status", status);
        msg.put("timestamp", System.currentTimeMillis());
        messagingTemplate.convertAndSend("/topic/vehicle-status/" + vehicleId, (Object) msg);
    }

    private double computeSpeed(int index, int total) {
        double pct = (double) index / (total - 1);

        double factor;
        if (pct < 0.15) {
            factor = pct / 0.15;
        } else if (pct > 0.85) {
            factor = (1.0 - pct) / 0.15;
        } else {
            factor = 1.0;
        }

        double jitter = Math.sin(index * 0.7) * 0.05;
        double speed = SPEED_MAX_KMH * (factor + jitter);
        speed = Math.max(SPEED_MIN_KMH * factor, Math.min(SPEED_MAX_KMH, speed));

        return Math.max(0.0, Math.round(speed * 10.0) / 10.0);
    }
}