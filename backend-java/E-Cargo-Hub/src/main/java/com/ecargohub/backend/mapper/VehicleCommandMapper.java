package com.ecargohub.backend.mapper;

import com.ecargohub.backend.dto.command.VehicleCommandDto;
import com.ecargohub.backend.entity.VehicleCommandEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VehicleCommandMapper {

    @Mapping(source = "vehicle.id", target = "vehicleId")
    VehicleCommandDto toDto(VehicleCommandEntity entity);
}
