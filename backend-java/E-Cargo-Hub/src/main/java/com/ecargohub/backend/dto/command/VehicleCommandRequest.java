package com.ecargohub.backend.dto.command;

import com.ecargohub.backend.domain.enums.VehicleCommandTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Petición de comando a un vehículo")
public record VehicleCommandRequest(

        @Schema(description = "Tipo de comando a ejecutar", example = "START", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "command is required") VehicleCommandTypeEnum command,

        @Schema(description = "Valor asociado al comando (opcional)", example = "1.0") Double value,

        @Schema(description = "Latitud del origen (opcional, si no se envía usa Cox)", example = "38.1408") @DecimalMin(value = "-90.0", message = "originLat must be >= -90") @DecimalMax(value = "90.0", message = "originLat must be <= 90") Double originLat,

        @Schema(description = "Longitud del origen (opcional, si no se envía usa Cox)", example = "-0.8844") @DecimalMin(value = "-180.0", message = "originLon must be >= -180") @DecimalMax(value = "180.0", message = "originLon must be <= 180") Double originLon,

        @Schema(description = "Latitud del destino (opcional, si no se envía usa Murcia)", example = "37.9922") @DecimalMin(value = "-90.0", message = "destLat must be >= -90") @DecimalMax(value = "90.0", message = "destLat must be <= 90") Double destLat,

        @Schema(description = "Longitud del destino (opcional, si no se envía usa Murcia)", example = "-1.1307") @DecimalMin(value = "-180.0", message = "destLon must be >= -180") @DecimalMax(value = "180.0", message = "destLon must be <= 180") Double destLon) {
}