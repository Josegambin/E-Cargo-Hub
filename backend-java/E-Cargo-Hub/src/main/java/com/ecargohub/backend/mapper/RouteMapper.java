package com.ecargohub.backend.mapper;

import com.ecargohub.backend.dto.route.CreateRouteRequest;
import com.ecargohub.backend.dto.route.RouteDto;
import com.ecargohub.backend.entity.RouteEntity;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = { GeoMapper.class }
)
public interface RouteMapper {

    RouteDto toDto(RouteEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "distanceMeters", constant = "0.0")
    @Mapping(target = "durationSeconds", constant = "0L")
    @Mapping(target = "status", constant = "CREATED")
    @Mapping(target = "geometry", ignore = true)
    RouteEntity toEntity(CreateRouteRequest request);
}