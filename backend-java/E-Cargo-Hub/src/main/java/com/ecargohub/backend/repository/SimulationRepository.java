package com.ecargohub.backend.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecargohub.backend.entity.SimulationEntity;

@Repository
public interface SimulationRepository extends JpaRepository<SimulationEntity, Long> {

    List<SimulationEntity> findByVehicleId(Long vehicleId);

    List<SimulationEntity> findByRouteId(Long routeId);
}
