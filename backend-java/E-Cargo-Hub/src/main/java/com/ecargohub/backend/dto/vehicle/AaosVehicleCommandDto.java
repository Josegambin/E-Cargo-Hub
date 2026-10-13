package com.ecargohub.backend.dto.vehicle;

import com.ecargohub.backend.domain.enums.VehicleCommandTypeEnum;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AaosVehicleCommandDto(UUID commandId, Long vehicleId, VehicleCommandTypeEnum command, Double value,
        String target, OffsetDateTime createdAt) {
}
