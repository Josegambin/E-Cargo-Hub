package com.ecargohub.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

@Service
public class SimulationRegistry {

    private static final Logger log = LoggerFactory.getLogger(SimulationRegistry.class);

    private final Map<Long, Future<?>> active = new ConcurrentHashMap<>();

    public boolean isRunning(Long vehicleId) {
        Future<?> f = active.get(vehicleId);
        return f != null && !f.isDone();
    }

    public void register(Long vehicleId, Future<?> future) {
        active.put(vehicleId, future);
        log.info("🟢 Simulación registrada para vehículo {}", vehicleId);
    }

    public void unregister(Long vehicleId, Future<?> future) {
        active.computeIfPresent(vehicleId, (k, current) -> current == future ? null : current);
        log.info("🔴 Simulación finalizada para vehículo {}", vehicleId);
    }

    public void cancel(Long vehicleId) {
        Future<?> f = active.remove(vehicleId);
        if (f != null) {
            log.warn("🛑 Cancelando simulación de vehículo {}", vehicleId);
            f.cancel(true);
        }
    }
}