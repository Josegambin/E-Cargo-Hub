package com.ecargohub.backend.repository;

import com.ecargohub.backend.entity.VehicleTelemetryEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface VehicleTelemetryRepository extends JpaRepository<VehicleTelemetryEntity, Long> {

    /** Todos los puntos de un vehículo, ordenados cronológicamente. */
    List<VehicleTelemetryEntity> findByVehicleIdOrderByRecordedAtAsc(Long vehicleId);

    /** Puntos en un rango de tiempo. */
    List<VehicleTelemetryEntity> findByVehicleIdAndRecordedAtBetweenOrderByRecordedAtAsc(Long vehicleId,
            OffsetDateTime from, OffsetDateTime to);

    /** Puntos desde una fecha, inclusive. */
    List<VehicleTelemetryEntity> findByVehicleIdAndRecordedAtGreaterThanEqualOrderByRecordedAtAsc(Long vehicleId,
            OffsetDateTime from);

    /** Puntos hasta una fecha, inclusive. */
    List<VehicleTelemetryEntity> findByVehicleIdAndRecordedAtLessThanEqualOrderByRecordedAtAsc(Long vehicleId,
            OffsetDateTime to);

    /** Últimos N puntos (más recientes primero). */
    List<VehicleTelemetryEntity> findByVehicleIdOrderByRecordedAtDesc(Long vehicleId, Pageable pageable);

    /** Borrar todo el histórico de un vehículo. */
    void deleteByVehicleId(Long vehicleId);
}