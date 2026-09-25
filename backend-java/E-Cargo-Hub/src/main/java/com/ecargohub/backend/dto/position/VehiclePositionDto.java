package com.ecargohub.backend.dto.position;

import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record VehiclePositionDto(
        Long vehicleId,
        Double latitude,
        Double longitude,
        Double speed,
        Double speedLimit,
        Double acceleration,
        Double heading,
        OffsetDateTime timestamp
) {}