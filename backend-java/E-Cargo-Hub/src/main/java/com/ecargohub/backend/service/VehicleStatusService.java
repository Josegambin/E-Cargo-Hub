package com.ecargohub.backend.service;

import com.ecargohub.backend.dto.status.VehicleStatusDto;
import com.ecargohub.backend.entity.VehicleCurrentStatusEntity;
import com.ecargohub.backend.repository.VehicleCurrentStatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class VehicleStatusService {

    private final VehicleCurrentStatusRepository repository;

    public VehicleStatusService(VehicleCurrentStatusRepository repository) {
        this.repository = repository;
    }

    /**
     * UPSERT: guarda o actualiza la posición actual del vehículo. Se usa REQUIRES_NEW para no verse afectado por
     * transacciones en curso (el consumer corre en su propio hilo, sin transacción externa).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatus(Long vehicleId, Double lat, Double lon, Integer progress, Double speed, String status) {
        VehicleCurrentStatusEntity entity = repository.findById(vehicleId).orElseGet(() -> {
            VehicleCurrentStatusEntity e = new VehicleCurrentStatusEntity();
            e.setVehicleId(vehicleId);
            return e;
        });
        entity.setLatitude(lat);
        entity.setLongitude(lon);
        entity.setProgress(progress);
        entity.setSpeedKmh(speed);
        entity.setStatus(status);
        entity.setUpdatedAt(OffsetDateTime.now());
        repository.save(entity);
    }

    @Transactional(readOnly = true)
    public Optional<VehicleStatusDto> getStatus(Long vehicleId) {
        return repository.findById(vehicleId).map(e -> new VehicleStatusDto(e.getVehicleId(), e.getLatitude(),
                e.getLongitude(), e.getProgress(), e.getSpeedKmh(), e.getStatus(), e.getUpdatedAt()));
    }
}