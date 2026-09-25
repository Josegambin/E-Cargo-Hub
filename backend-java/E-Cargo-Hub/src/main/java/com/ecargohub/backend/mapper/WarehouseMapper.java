package com.ecargohub.backend.mapper;

import com.ecargohub.backend.dto.warehouse.CreateWarehouseRequest;
import com.ecargohub.backend.dto.warehouse.UpdateWarehouseRequest;
import com.ecargohub.backend.dto.warehouse.WarehouseDto;
import com.ecargohub.backend.entity.WarehouseEntity;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = { GeoMapper.class }
)
public interface WarehouseMapper {

    WarehouseDto toDto(WarehouseEntity entity);

    WarehouseEntity toEntity(CreateWarehouseRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget WarehouseEntity entity, UpdateWarehouseRequest request);
}