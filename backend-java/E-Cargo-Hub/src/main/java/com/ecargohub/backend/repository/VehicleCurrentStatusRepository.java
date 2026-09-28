package com.ecargohub.backend.repository;

import com.ecargohub.backend.entity.VehicleCurrentStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleCurrentStatusRepository extends JpaRepository<VehicleCurrentStatusEntity, Long> {
}