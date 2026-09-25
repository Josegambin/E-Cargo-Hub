package com.ecargohub.backend.dto.simulation;

import jakarta.validation.constraints.*;
import lombok.Builder;

@Builder
public record CreateSimulationRequest(

        @NotBlank
        @Size(min = 1, max = 150)
        String name,

        @NotNull
        Long vehicleId,

        @NotNull
        Long routeId,

        @DecimalMin("0.0")
        Double simulationSpeed
) {}