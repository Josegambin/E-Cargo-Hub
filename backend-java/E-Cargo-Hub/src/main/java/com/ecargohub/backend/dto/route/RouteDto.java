package com.ecargohub.backend.dto.route;

import com.ecargohub.backend.domain.enums.RouteStatusEnum;
import com.ecargohub.backend.dto.geo.GeoLineStringDto;
import com.ecargohub.backend.dto.geo.GeoPointDto;
import lombok.Builder;

@Builder
public record RouteDto(
        Long id,
        String name,
        GeoPointDto origin,
        GeoPointDto destination,
        Double distanceMeters,
        Long durationSeconds,
        RouteStatusEnum status,
        GeoLineStringDto geometry
) {}