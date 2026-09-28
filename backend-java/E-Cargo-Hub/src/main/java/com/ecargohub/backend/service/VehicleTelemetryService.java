package com.ecargohub.backend.service;

import com.ecargohub.backend.dto.telemetry.TelemetryPointDto;
import com.ecargohub.backend.entity.VehicleTelemetryEntity;
import com.ecargohub.backend.repository.VehicleTelemetryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class VehicleTelemetryService {

    private static final Logger log = LoggerFactory.getLogger(VehicleTelemetryService.class);

    private final VehicleTelemetryRepository repository;

    public VehicleTelemetryService(VehicleTelemetryRepository repository) {
        this.repository = repository;
    }

    /**
     * Guarda un punto de telemetría. REQUIRES_NEW para que no interfiera con la transacción del consumer.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void savePoint(Long vehicleId, Double lat, Double lon, Integer progress, Double speed, String status) {
        VehicleTelemetryEntity e = new VehicleTelemetryEntity();
        e.setVehicleId(vehicleId);
        e.setLatitude(lat);
        e.setLongitude(lon);
        e.setProgress(progress);
        e.setSpeedKmh(speed);
        e.setStatus(status);
        e.setRecordedAt(OffsetDateTime.now());
        repository.save(e);
    }

    @Transactional(readOnly = true)
    public List<TelemetryPointDto> getAll(Long vehicleId) {
        return repository.findByVehicleIdOrderByRecordedAtAsc(vehicleId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<TelemetryPointDto> getRange(Long vehicleId, OffsetDateTime from, OffsetDateTime to) {
        return repository.findByVehicleIdAndRecordedAtBetweenOrderByRecordedAtAsc(vehicleId, from, to).stream()
                .map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<TelemetryPointDto> getLatest(Long vehicleId, int limit) {
        List<VehicleTelemetryEntity> latest = repository.findByVehicleIdOrderByRecordedAtDesc(vehicleId,
                PageRequest.of(0, limit));
        // los devolvemos en orden cronológico ascendente
        return latest.stream().sorted((a, b) -> a.getRecordedAt().compareTo(b.getRecordedAt())).map(this::toDto)
                .toList();
    }

    @Transactional
    public long deleteAll(Long vehicleId) {
        long count = repository.findByVehicleIdOrderByRecordedAtAsc(vehicleId).size();
        repository.deleteByVehicleId(vehicleId);
        log.info("🗑️ Borrados {} puntos de telemetría para vehículo {}", count, vehicleId);
        return count;
    }

    private TelemetryPointDto toDto(VehicleTelemetryEntity e) {
        return new TelemetryPointDto(e.getId(), e.getVehicleId(), e.getLatitude(), e.getLongitude(), e.getProgress(),
                e.getSpeedKmh(), e.getStatus(), e.getRecordedAt());
    }
}