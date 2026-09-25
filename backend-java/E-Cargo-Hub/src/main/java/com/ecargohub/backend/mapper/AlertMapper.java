package com.ecargohub.backend.mapper;

import com.ecargohub.backend.dto.alert.VehicleAlertDto;
import com.ecargohub.backend.entity.VehicleAlertEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AlertMapper {

    @Mapping(source = "vehicle.id", target = "vehicleId")
    @Mapping(source = "simulation.id", target = "simulationId")
    VehicleAlertDto toDto(VehicleAlertEntity entity);
}