package com.ecargohub.backend.repository;

import com.ecargohub.backend.entity.AaosTelemetryEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AaosTelemetryRepository extends JpaRepository<AaosTelemetryEntity, Long> {

    List<AaosTelemetryEntity> findByVehicleIdOrderByReceivedAtDesc(Long vehicleId, Pageable pageable);
}
