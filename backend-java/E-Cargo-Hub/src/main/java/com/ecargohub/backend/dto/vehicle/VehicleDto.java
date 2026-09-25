package com.ecargohub.backend.dto.vehicle;

import com.ecargohub.backend.domain.VehicleStatus;
import com.ecargohub.backend.domain.VehicleType;
import com.ecargohub.backend.dto.position.VehiclePositionDto;
import lombok.Builder;

@Builder
public record VehicleDto(
        Long id,
        String name,
        VehicleType type,
        Double maxSpeed,
        VehicleStatus status,
        VehiclePositionDto currentPosition
) {}