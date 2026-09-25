package com.ecargohub.backend.mapper;

import com.ecargohub.backend.dto.position.VehiclePositionDto;
import com.ecargohub.backend.dto.vehicle.CreateVehicleRequest;
import com.ecargohub.backend.dto.vehicle.UpdateVehicleRequest;
import com.ecargohub.backend.dto.vehicle.VehicleDto;
import com.ecargohub.backend.entity.VehicleEntity;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = { PositionMapper.class }
)
public interface VehicleMapper {

    VehicleDto toDto(VehicleEntity entity);

    VehicleEntity toEntity(CreateVehicleRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget VehicleEntity entity, UpdateVehicleRequest request);
}