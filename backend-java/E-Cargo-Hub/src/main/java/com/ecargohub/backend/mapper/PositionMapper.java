package com.ecargohub.backend.mapper;

import com.ecargohub.backend.dto.position.VehiclePositionDto;
import com.ecargohub.backend.entity.VehiclePositionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PositionMapper {

    @Mapping(source = "vehicle.id", target = "vehicleId")
    VehiclePositionDto toDto(VehiclePositionEntity entity);
}