package com.ecargohub.backend.dto.command;

import com.ecargohub.backend.domain.enums.VehicleCommandTypeEnum;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record VehicleCommandDto(Long id, UUID commandId, Long vehicleId, VehicleCommandTypeEnum command, Double value,
        OffsetDateTime createdAt) {
}