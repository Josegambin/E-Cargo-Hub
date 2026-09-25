package com.ecargohub.backend.mapper;

import com.ecargohub.backend.dto.simulation.CreateSimulationRequest;
import com.ecargohub.backend.dto.simulation.SimulationDto;
import com.ecargohub.backend.entity.RouteEntity;
import com.ecargohub.backend.entity.SimulationEntity;
import com.ecargohub.backend.entity.VehicleEntity;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface SimulationMapper {

    @Mapping(source = "vehicle.id", target = "vehicleId")
    @Mapping(source = "route.id", target = "routeId")
    SimulationDto toDto(SimulationEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "CREATED")
    @Mapping(target = "startedAt", ignore = true)
    @Mapping(target = "finishedAt", ignore = true)
    @Mapping(target = "vehicle", source = "vehicle")
    @Mapping(target = "route", source = "route")
    
    // ✅ CORRECCIÓN: Rompe la ambigüedad indicando que el 'name' proviene de la request
    @Mapping(target = "name", source = "request.name")
    
    @Mapping(target = "simulationSpeed",
             expression = "java(request.simulationSpeed() != null ? request.simulationSpeed() : 1.0)")
    SimulationEntity toEntity(CreateSimulationRequest request,
                              VehicleEntity vehicle,
                              RouteEntity route);
}
