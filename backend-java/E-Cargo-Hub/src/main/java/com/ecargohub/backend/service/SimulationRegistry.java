package com.ecargohub.backend.service;

import com.ecargohub.backend.domain.enums.VehicleStatusEnum;
import com.ecargohub.backend.domain.enums.VehicleCommandTypeEnum;
import com.ecargohub.backend.dto.vehicle.VehicleControlStateDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

@Service
public class SimulationRegistry {

    private static final Logger log = LoggerFactory.getLogger(SimulationRegistry.class);

    private final Map<Long, SimulationHandle> active = new ConcurrentHashMap<>();
    private final Map<Long, VehicleControlStateDto> controls = new ConcurrentHashMap<>();
    private final Map<Long, List<double[]>> routes = new ConcurrentHashMap<>();

    public void setRouteCoordinates(Long vehicleId, List<double[]> coordinates) {
        routes.put(vehicleId, copyCoordinates(coordinates));
    }

    public List<double[]> getRouteCoordinates(Long vehicleId) {
        return copyCoordinates(routes.getOrDefault(vehicleId, List.of()));
    }

    private List<double[]> copyCoordinates(List<double[]> coordinates) {
        return coordinates.stream().map(double[]::clone).toList();
    }

    public VehicleControlStateDto getControls(Long vehicleId) {
        return controls.computeIfAbsent(vehicleId, this::defaultControls);
    }

    public VehicleControlStateDto updateControl(Long vehicleId, VehicleCommandTypeEnum command, double value) {
        return controls.compute(vehicleId, (id, current) -> {
            VehicleControlStateDto state = current == null ? defaultControls(id) : current;
            return switch (command) {
            case SET_IGNITION -> new VehicleControlStateDto(id, value == 1.0, state.headlightsOn(), state.highBeamsOn(),
                    state.hazardLightsOn(), state.turnSignal(), state.wipersOn(), state.climateOn(),
                    state.climateTemperatureC(), state.climateFanSpeed(), state.climateMode());
            case SET_HEADLIGHTS -> new VehicleControlStateDto(id, state.ignitionOn(), value == 1.0, state.highBeamsOn(),
                    state.hazardLightsOn(), state.turnSignal(), state.wipersOn(), state.climateOn(),
                    state.climateTemperatureC(), state.climateFanSpeed(), state.climateMode());
            case SET_HIGH_BEAMS -> new VehicleControlStateDto(id, state.ignitionOn(), state.headlightsOn(),
                    value == 1.0, state.hazardLightsOn(), state.turnSignal(), state.wipersOn(), state.climateOn(),
                    state.climateTemperatureC(), state.climateFanSpeed(), state.climateMode());
            case SET_HAZARD_LIGHTS -> new VehicleControlStateDto(id, state.ignitionOn(), state.headlightsOn(),
                    state.highBeamsOn(), value == 1.0, state.turnSignal(), state.wipersOn(), state.climateOn(),
                    state.climateTemperatureC(), state.climateFanSpeed(), state.climateMode());
            case SET_TURN_SIGNAL -> new VehicleControlStateDto(id, state.ignitionOn(), state.headlightsOn(),
                    state.highBeamsOn(), state.hazardLightsOn(), value == 1.0 ? "LEFT" : value == 2.0 ? "RIGHT" : "OFF",
                    state.wipersOn(), state.climateOn(), state.climateTemperatureC(), state.climateFanSpeed(),
                    state.climateMode());
            case SET_WIPERS -> new VehicleControlStateDto(id, state.ignitionOn(), state.headlightsOn(),
                    state.highBeamsOn(), state.hazardLightsOn(), state.turnSignal(), value == 1.0, state.climateOn(),
                    state.climateTemperatureC(), state.climateFanSpeed(), state.climateMode());
            case SET_CLIMATE_POWER -> new VehicleControlStateDto(id, state.ignitionOn(), state.headlightsOn(),
                    state.highBeamsOn(), state.hazardLightsOn(), state.turnSignal(), state.wipersOn(), value == 1.0,
                    state.climateTemperatureC(), state.climateFanSpeed(), state.climateMode());
            case SET_CLIMATE_TEMPERATURE -> new VehicleControlStateDto(id, state.ignitionOn(), state.headlightsOn(),
                    state.highBeamsOn(), state.hazardLightsOn(), state.turnSignal(), state.wipersOn(),
                    state.climateOn(), value, state.climateFanSpeed(), state.climateMode());
            case SET_CLIMATE_FAN_SPEED -> new VehicleControlStateDto(id, state.ignitionOn(), state.headlightsOn(),
                    state.highBeamsOn(), state.hazardLightsOn(), state.turnSignal(), state.wipersOn(),
                    state.climateOn(), state.climateTemperatureC(), (int) value, state.climateMode());
            case SET_CLIMATE_MODE -> new VehicleControlStateDto(id, state.ignitionOn(), state.headlightsOn(),
                    state.highBeamsOn(), state.hazardLightsOn(), state.turnSignal(), state.wipersOn(),
                    state.climateOn(), state.climateTemperatureC(), state.climateFanSpeed(),
                    value == 0.0 ? "AUTO" : value == 1.0 ? "COOL" : "HEAT");
            default -> throw new IllegalArgumentException("Not a vehicle control command: " + command);
            };
        });
    }

    private VehicleControlStateDto defaultControls(Long vehicleId) {
        return new VehicleControlStateDto(vehicleId, true, false, false, false, "OFF", false, false, 22.0, 0, "AUTO");
    }

    public boolean isRunning(Long vehicleId) {
        SimulationHandle h = active.get(vehicleId);
        return h != null && !h.isFinished();
    }

    public SimulationHandle register(Long vehicleId, Future<?> future) {
        SimulationHandle handle = new SimulationHandle(vehicleId, future);
        active.put(vehicleId, handle);
        log.info("🟢 Simulación registrada para vehículo {}", vehicleId);
        return handle;
    }

    public void unregister(Long vehicleId, SimulationHandle handle) {
        active.computeIfPresent(vehicleId, (k, current) -> current == handle ? null : current);
        log.info("🔴 Simulación desregistrada para vehículo {}", vehicleId);
    }

    public void cancel(Long vehicleId) {
        SimulationHandle h = active.remove(vehicleId);
        if (h != null) {
            h.requestStop();
            h.getFuture().cancel(true);
            log.warn("🛑 Simulación cancelada para vehículo {}", vehicleId);
        }
    }

    public SimulationHandle get(Long vehicleId) {
        return active.get(vehicleId);
    }

    public VehicleStatusEnum getState(Long vehicleId) {
        SimulationHandle h = active.get(vehicleId);
        if (h == null)
            return VehicleStatusEnum.IDLE;
        return h.getState();
    }

    // -------- Handler interno --------

    public static class SimulationHandle {
        private final Long vehicleId;
        private final Future<?> future;
        private volatile boolean paused = false;
        private volatile boolean stopped = false;
        private volatile VehicleStatusEnum state = VehicleStatusEnum.EN_RUTA;
        private volatile Double targetSpeedKmh;
        private volatile double currentSpeedKmh;

        public SimulationHandle(Long vehicleId, Future<?> future) {
            this.vehicleId = vehicleId;
            this.future = future;
        }

        public Long getVehicleId() {
            return vehicleId;
        }

        public Future<?> getFuture() {
            return future;
        }

        public boolean isPaused() {
            return paused;
        }

        public boolean isStopped() {
            return stopped;
        }

        public VehicleStatusEnum getState() {
            return state;
        }

        public void setState(VehicleStatusEnum s) {
            this.state = s;
        }

        public Double getTargetSpeedKmh() {
            return targetSpeedKmh;
        }

        public void setTargetSpeedKmh(double speedKmh) {
            this.targetSpeedKmh = speedKmh;
        }

        public double getCurrentSpeedKmh() {
            return currentSpeedKmh;
        }

        public void setCurrentSpeedKmh(double speedKmh) {
            this.currentSpeedKmh = speedKmh;
        }

        public void pause() {
            this.paused = true;
            this.state = VehicleStatusEnum.PAUSADO;
        }

        public void resume() {
            this.paused = false;
            this.state = VehicleStatusEnum.EN_RUTA;
        }

        public void requestStop() {
            this.stopped = true;
        }

        public boolean isFinished() {
            return future.isDone() || future.isCancelled();
        }
    }
}