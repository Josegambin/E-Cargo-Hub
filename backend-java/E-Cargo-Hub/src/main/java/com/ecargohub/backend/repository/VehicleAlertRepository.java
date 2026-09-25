package com.ecargohub.backend.repository;

import com.ecargohub.backend.domain.enums.AlertSeverityEnum;
import com.ecargohub.backend.entity.VehicleAlertEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface VehicleAlertRepository extends JpaRepository<VehicleAlertEntity, Long> {

    List<VehicleAlertEntity> findByVehicleId(Long vehicleId);

    List<VehicleAlertEntity> findBySimulationId(Long simulationId);

    List<VehicleAlertEntity> findBySeverity(AlertSeverityEnum severity);

    List<VehicleAlertEntity> findByVehicleIdAndSimulationIdAndSeverity(
            Long vehicleId, Long simulationId, AlertSeverityEnum severity);
}

