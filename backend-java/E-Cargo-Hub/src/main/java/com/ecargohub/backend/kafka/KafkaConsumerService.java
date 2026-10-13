package com.ecargohub.backend.kafka;

import com.ecargohub.backend.domain.enums.VehicleCommandTypeEnum;
import com.ecargohub.backend.domain.enums.VehicleEnergyTypeEnum;
import com.ecargohub.backend.domain.enums.VehicleStatusEnum;
import com.ecargohub.backend.domain.enums.AlertSeverityEnum;
import com.ecargohub.backend.domain.enums.AlertTypeEnum;
import com.ecargohub.backend.dto.alert.VehicleAlertDto;
import com.ecargohub.backend.dto.command.VehicleCommandDto;
import com.ecargohub.backend.dto.route.RouteResponseDto;
import com.ecargohub.backend.dto.vehicle.VehicleControlStateDto;
import com.ecargohub.backend.service.FleetEventService;
import com.ecargohub.backend.service.IdempotencyService;
import com.ecargohub.backend.service.SimulationRegistry;
import com.ecargohub.backend.service.SimulationRegistry.SimulationHandle;
import com.ecargohub.backend.service.VehicleStatusService;
import com.ecargohub.backend.service.VehicleTelemetryService;
import com.ecargohub.backend.service.VehicleEnergyService;
import com.ecargohub.backend.service.geo.GraphHopperService;
import com.ecargohub.backend.repository.VehicleRepository;
import com.ecargohub.backend.interfaces.AlertService;
import tools.jackson.databind.json.JsonMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.OffsetDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

@Service
public class KafkaConsumerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerService.class);

    private static final long STEP_DELAY_MS = 1000;
    private static final double ACCELERATION_KMH_PER_STEP = 5.0;

    private final GraphHopperService graphHopperService;
    private final SimpMessagingTemplate messagingTemplate;
    private final IdempotencyService idempotencyService;
    private final JsonMapper objectMapper;
    private final ExecutorService simulationExecutor;
    private final SimulationRegistry simulationRegistry;
    private final VehicleStatusService vehicleStatusService;
    private final VehicleTelemetryService vehicleTelemetryService;
    private final FleetEventService fleetEventService;
    private final VehicleRepository vehicleRepository;
    private final VehicleEnergyService vehicleEnergyService;
    private final AlertService alertService;

    @PreDestroy
    void shutdownSimulationExecutor() {
        simulationExecutor.shutdownNow();
    }

    public KafkaConsumerService(GraphHopperService graphHopperService, SimpMessagingTemplate messagingTemplate,
            IdempotencyService idempotencyService, SimulationRegistry simulationRegistry,
            VehicleStatusService vehicleStatusService, VehicleTelemetryService vehicleTelemetryService,
            FleetEventService fleetEventService, VehicleRepository vehicleRepository,
            VehicleEnergyService vehicleEnergyService, AlertService alertService) {
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
        this.vehicleRepository = vehicleRepository;
        this.vehicleEnergyService = vehicleEnergyService;
        this.alertService = alertService;
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
            case SET_SPEED, INCREASE_SPEED, DECREASE_SPEED -> handleSetSpeed(command);
            case EMERGENCY_BRAKE -> handleEmergencyBrake(command.vehicleId());
            case SET_IGNITION, SET_HEADLIGHTS, SET_HIGH_BEAMS, SET_HAZARD_LIGHTS, SET_TURN_SIGNAL, SET_WIPERS, SET_CLIMATE_POWER, SET_CLIMATE_TEMPERATURE, SET_CLIMATE_FAN_SPEED, SET_CLIMATE_MODE -> handleVehicleControl(
                    command);
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

        if (!simulationRegistry.getControls(vehicleId).ignitionOn()) {
            log.warn("No se inicia el vehículo {} porque el encendido simulado está apagado", vehicleId);
            return;
        }
        if (simulationRegistry.isRunning(vehicleId)) {
            log.warn("⚠️ Vehículo {} ya tiene una simulación activa. Ignorando START.", vehicleId);
            return;
        }

        double maxSpeedKmh = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found: " + vehicleId)).getMaxSpeed();
        RouteResponseDto route = buildRoute(command, maxSpeedKmh);
        simulationRegistry.setRouteCoordinates(vehicleId, route.coordinates());
        final SimulationHandle[] holder = new SimulationHandle[1];
        FutureTask<Void> future = new FutureTask<>(
                () -> simulateVehicleMovement(vehicleId, route, holder[0], maxSpeedKmh), null);
        holder[0] = simulationRegistry.register(vehicleId, future);
        simulationExecutor.execute(future);

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

    private void handleSetSpeed(VehicleCommandDto command) {
        SimulationHandle handle = simulationRegistry.get(command.vehicleId());
        if (handle == null || command.value() == null) {
            log.warn("No se puede cambiar la velocidad del vehículo {}", command.vehicleId());
            return;
        }
        handle.setTargetSpeedKmh(command.value());
        log.info("Velocidad objetivo del vehículo {} establecida a {} km/h", command.vehicleId(), command.value());
        messagingTemplate.convertAndSend("/topic/vehicles/" + command.vehicleId() + "/commands",
                new com.ecargohub.backend.dto.vehicle.AaosVehicleCommandDto(command.commandId(), command.vehicleId(),
                        command.command(), command.value(), "SIMULATOR_ONLY", command.createdAt()));
    }

    private void handleEmergencyBrake(Long vehicleId) {
        SimulationHandle handle = simulationRegistry.get(vehicleId);
        if (handle == null) {
            log.warn("Frenada de emergencia simulada ignorada: vehículo {} no está activo", vehicleId);
            return;
        }
        handle.requestStop();
        log.warn("Frenada de emergencia aplicada únicamente a la simulación del vehículo {}", vehicleId);
        messagingTemplate.convertAndSend("/topic/vehicles/" + vehicleId + "/commands",
                new com.ecargohub.backend.dto.vehicle.AaosVehicleCommandDto(java.util.UUID.randomUUID(), vehicleId,
                        VehicleCommandTypeEnum.EMERGENCY_BRAKE, 0.0, "SIMULATOR_ONLY", java.time.OffsetDateTime.now()));
    }

    private void handleVehicleControl(VehicleCommandDto command) {
        if (command.value() == null) {
            log.warn("Control {} sin valor para vehículo {}", command.command(), command.vehicleId());
            return;
        }
        VehicleControlStateDto state = simulationRegistry.updateControl(command.vehicleId(), command.command(),
                command.value());
        if (command.command() == VehicleCommandTypeEnum.SET_IGNITION && command.value() == 0.0) {
            SimulationHandle handle = simulationRegistry.get(command.vehicleId());
            if (handle != null && !handle.isFinished()) {
                handle.requestStop();
            }
        }
        messagingTemplate.convertAndSend("/topic/vehicles/" + command.vehicleId() + "/control-state", state);
        messagingTemplate.convertAndSend("/topic/vehicles/" + command.vehicleId() + "/commands",
                new com.ecargohub.backend.dto.vehicle.AaosVehicleCommandDto(command.commandId(), command.vehicleId(),
                        command.command(), command.value(), "SIMULATOR_ONLY", command.createdAt()));
    }

    // ==========================================
    // RUTAS (parametrizadas con fallback)
    // ==========================================

    private RouteResponseDto buildRoute(VehicleCommandDto command, double vehicleMaxSpeedKmh) {
        if (command.originLat() == null || command.originLon() == null || command.destLat() == null
                || command.destLon() == null) {
            throw new IllegalArgumentException("A registered route is required to start a vehicle");
        }
        RouteResponseDto route = graphHopperService.calculateRoute(command.originLat(), command.originLon(),
                command.destLat(), command.destLon(), vehicleMaxSpeedKmh);
        if (route.coordinates() == null || route.coordinates().size() < 2 || route.speedLimitsKmh() == null
                || route.speedLimitsKmh().size() != route.coordinates().size() - 1) {
            throw new IllegalStateException("The route is missing complete road speed-limit data");
        }
        return route;
    }

    // ==========================================
    // SIMULACIÓN DEL MOVIMIENTO
    // ==========================================

    private void simulateVehicleMovement(Long vehicleId, RouteResponseDto route, SimulationHandle handle,
            double maxSpeedKmh) {
        List<double[]> coords = route.coordinates();
        int total = coords.size();
        List<Double> roadSpeedLimits = route.speedLimitsKmh();
        VehicleEnergyTypeEnum energyType = vehicleRepository.findById(vehicleId).map(vehicle -> vehicle.getEnergyType())
                .orElse(VehicleEnergyTypeEnum.BATTERY_ELECTRIC);

        double finalLat = coords.get(total - 1)[0];
        double finalLon = coords.get(total - 1)[1];
        double energyPercent = vehicleEnergyService.getEnergyPercent(vehicleId);
        boolean lowEnergyAlertSent = false;

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

            double roadLimit = Math.min(maxSpeedKmh, roadSpeedLimits.get(Math.min(i, roadSpeedLimits.size() - 1)));
            double requestedSpeed = handle.getTargetSpeedKmh() == null ? roadLimit
                    : Math.min(roadLimit, handle.getTargetSpeedKmh());
            double speed = approach(handle.getCurrentSpeedKmh(), requestedSpeed, ACCELERATION_KMH_PER_STEP);
            handle.setCurrentSpeedKmh(speed);
            if (speed == 0.0) {
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
            String gearPosition = gearForSpeed(speed);
            if (i > 0) {
                energyPercent = vehicleEnergyService.consumeDistance(vehicleId,
                        distanceMeters(coords.get(i - 1), coord));
            }

            Map<String, Object> telemetry = new HashMap<>();
            telemetry.put("vehicleId", vehicleId);
            telemetry.put("latitude", coord[0]);
            telemetry.put("longitude", coord[1]);
            telemetry.put("progress", progress);
            telemetry.put("speedKmh", speed);
            telemetry.put("speedLimitKmh", roadLimit);
            telemetry.put("energyPercent", energyPercent);
            telemetry.put("energyType", energyType.name());
            telemetry.put("gearPosition", gearPosition);
            telemetry.put("status", "EN_RUTA");
            telemetry.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend("/topic/vehicle-status/" + vehicleId, (Object) telemetry);

            try {
                vehicleStatusService.updateStatus(vehicleId, coord[0], coord[1], progress, speed, "EN_RUTA",
                        energyPercent, energyType, gearPosition);
            } catch (Exception e) {
                log.warn("No se pudo persistir última posición: {}", e.getMessage());
            }

            if (!lowEnergyAlertSent && energyPercent <= 10.0) {
                lowEnergyAlertSent = true;
                String energyName = energyType == VehicleEnergyTypeEnum.BATTERY_ELECTRIC ? "batería" : "combustible";
                try {
                    alertService.create(new VehicleAlertDto(null, vehicleId, null, AlertTypeEnum.LOW_ENERGY,
                            AlertSeverityEnum.WARNING, "Nivel de " + energyName + " del vehículo por debajo del 10%.",
                            speed, roadLimit, OffsetDateTime.now()));
                } catch (Exception alertError) {
                    log.error("No se pudo publicar la alerta de energía baja del vehículo {}", vehicleId, alertError);
                }
            }
            if (energyPercent <= 0.0) {
                handle.requestStop();
            }

            try {
                vehicleTelemetryService.savePoint(vehicleId, coord[0], coord[1], progress, speed, "EN_RUTA");
            } catch (Exception e) {
                log.warn("No se pudo persistir punto de telemetría: {}", e.getMessage());
            }

            try {
                long remainingDelay = speed > 0
                        ? Math.max(200, Math.min(6000, Math.round(STEP_DELAY_MS * 95.0 / speed))) : STEP_DELAY_MS;
                while (remainingDelay > 0 && !handle.isStopped() && !handle.isPaused()
                        && (handle.getTargetSpeedKmh() == null || handle.getTargetSpeedKmh() > 0.0)) {
                    long sleep = Math.min(200, remainingDelay);
                    Thread.sleep(sleep);
                    remainingDelay -= sleep;
                }
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
        finalStatus.put("energyPercent", energyPercent);
        finalStatus.put("energyType", energyType.name());
        finalStatus.put("gearPosition", "P");
        finalStatus.put("status", "COMPLETADO");
        finalStatus.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/vehicle-status/" + vehicleId, (Object) finalStatus);

        try {
            vehicleStatusService.updateStatus(vehicleId, finalLat, finalLon, 100, 0.0, "COMPLETADO", energyPercent,
                    energyType, "P");
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
        double energyPercent = vehicleEnergyService.getEnergyPercent(vehicleId);
        VehicleEnergyTypeEnum energyType = vehicleRepository.findById(vehicleId).map(vehicle -> vehicle.getEnergyType())
                .orElse(VehicleEnergyTypeEnum.BATTERY_ELECTRIC);
        Map<String, Object> stopped = new HashMap<>();
        stopped.put("vehicleId", vehicleId);
        stopped.put("latitude", lastCoord[0]);
        stopped.put("longitude", lastCoord[1]);
        stopped.put("progress", null);
        stopped.put("speedKmh", 0.0);
        stopped.put("energyPercent", energyPercent);
        stopped.put("energyType", energyType.name());
        stopped.put("gearPosition", "P");
        stopped.put("status", "STOPPED");
        stopped.put("timestamp", System.currentTimeMillis());
        messagingTemplate.convertAndSend("/topic/vehicle-status/" + vehicleId, (Object) stopped);

        try {
            vehicleStatusService.updateStatus(vehicleId, lastCoord[0], lastCoord[1], 0, 0.0, "STOPPED", energyPercent,
                    energyType, "P");
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

    private double approach(double current, double target, double step) {
        if (current < target) {
            return Math.min(current + step, target);
        }
        return Math.max(current - step, target);
    }

    private String gearForSpeed(double speedKmh) {
        if (speedKmh < 1.0) {
            return "P";
        }
        if (speedKmh < 15.0) {
            return "D1";
        }
        if (speedKmh < 30.0) {
            return "D2";
        }
        if (speedKmh < 50.0) {
            return "D3";
        }
        if (speedKmh < 70.0) {
            return "D4";
        }
        if (speedKmh < 90.0) {
            return "D5";
        }
        return "D6";
    }

    private double distanceMeters(double[] from, double[] to) {
        double earthRadiusMeters = 6_371_000.0;
        double lat1 = Math.toRadians(from[0]);
        double lat2 = Math.toRadians(to[0]);
        double deltaLat = lat2 - lat1;
        double deltaLon = Math.toRadians(to[1] - from[1]);
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        return earthRadiusMeters * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}