package com.ecargohub.backend.dto.command;

import com.ecargohub.backend.domain.enums.VehicleCommandTypeEnum;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record VehicleCommandDto(
        Long id,
        Long vehicleId,
        VehicleCommandTypeEnum command,
        Double value,
        OffsetDateTime createdAt
) {}