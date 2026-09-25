package com.ecargohub.backend.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecargohub.backend.entity.VehiclePositionEntity;
import java.util.Optional;


@Repository
public interface VehiclePositionRepository extends JpaRepository<VehiclePositionEntity, Long> {

    Optional<VehiclePositionEntity> findTopByVehicleIdOrderByTimestampDesc(Long vehicleId);

}

