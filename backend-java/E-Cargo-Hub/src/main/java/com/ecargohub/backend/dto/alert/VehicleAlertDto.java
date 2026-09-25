package com.ecargohub.backend.dto.alert;

import com.ecargohub.backend.domain.enums.AlertSeverityEnum;
import com.ecargohub.backend.domain.enums.AlertTypeEnum;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record VehicleAlertDto(
        Long id,
        Long vehicleId,
        Long simulationId,
        AlertTypeEnum type,
        AlertSeverityEnum severity,
        String message,
        Double currentSpeed,
        Double speedLimit,
        OffsetDateTime timestamp
) {}