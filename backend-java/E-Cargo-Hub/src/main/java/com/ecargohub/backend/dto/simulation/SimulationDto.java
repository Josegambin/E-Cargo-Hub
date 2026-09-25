package com.ecargohub.backend.dto.simulation;

import com.ecargohub.backend.domain.enums.SimulationStatusEnum;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record SimulationDto(
        Long id,
        String name,
        Long vehicleId,
        Long routeId,
        SimulationStatusEnum status,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        Double simulationSpeed
) {}