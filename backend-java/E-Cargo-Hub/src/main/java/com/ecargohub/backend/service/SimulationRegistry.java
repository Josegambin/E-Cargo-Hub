package com.ecargohub.backend.service;

import com.ecargohub.backend.domain.enums.VehicleStatusEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

@Service
public class SimulationRegistry {

    private static final Logger log = LoggerFactory.getLogger(SimulationRegistry.class);

    private final Map<Long, SimulationHandle> active = new ConcurrentHashMap<>();

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