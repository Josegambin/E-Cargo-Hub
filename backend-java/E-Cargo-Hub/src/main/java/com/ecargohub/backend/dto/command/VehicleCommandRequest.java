package com.ecargohub.backend.dto.command;

import com.ecargohub.backend.domain.enums.VehicleCommandTypeEnum;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record VehicleCommandRequest(

        @NotNull(message = "command is required") VehicleCommandTypeEnum command,

        Double value,

        @DecimalMin(value = "-90.0", message = "originLat must be >= -90") @DecimalMax(value = "90.0", message = "originLat must be <= 90") Double originLat,

        @DecimalMin(value = "-180.0", message = "originLon must be >= -180") @DecimalMax(value = "180.0", message = "originLon must be <= 180") Double originLon,

        @DecimalMin(value = "-90.0", message = "destLat must be >= -90") @DecimalMax(value = "90.0", message = "destLat must be <= 90") Double destLat,

        @DecimalMin(value = "-180.0", message = "destLon must be >= -180") @DecimalMax(value = "180.0", message = "destLon must be <= 180") Double destLon) {
}