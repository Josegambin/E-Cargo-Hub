package com.ecargohub.backend.dto.vehicle;

import com.ecargohub.backend.domain.VehicleType;
import jakarta.validation.constraints.*;
import lombok.Builder;

@Builder
public record CreateVehicleRequest(

        @NotBlank
        @Size(min = 1, max = 100)
        String name,

        @NotNull
        VehicleType type,

        @NotNull
        @DecimalMin("0.0")
        Double maxSpeed
) {}