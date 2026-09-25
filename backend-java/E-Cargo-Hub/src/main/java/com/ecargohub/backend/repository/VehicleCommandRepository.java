package com.ecargohub.backend.repository;

import com.ecargohub.backend.entity.VehicleCommandEntity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleCommandRepository extends JpaRepository<VehicleCommandEntity, Long> {
        List<VehicleCommandEntity> findByVehicleIdOrderByCreatedAtDesc(Long vehicleId);

}
