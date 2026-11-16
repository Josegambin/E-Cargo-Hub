package com.ecargohub.backend.dto.vhal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record VhalWriteRequest(
        @Schema(description = "Nombre de propiedad VHAL, por ejemplo PERF_VEHICLE_SPEED o FUEL_LEVEL", example = "PERF_VEHICLE_SPEED") @NotBlank String property,
        @Schema(description = "Identificador de área (0 para propiedades globales)", example = "0") int areaId,
        @Schema(description = "Valor a inyectar en el VHAL", example = "13.9") double value) {
}
