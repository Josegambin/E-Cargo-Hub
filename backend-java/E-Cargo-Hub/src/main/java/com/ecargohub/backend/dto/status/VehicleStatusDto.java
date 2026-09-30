package com.ecargohub.backend.dto.status;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Estado actual de un vehículo")
public record VehicleStatusDto(

        @Schema(description = "ID del vehículo", example = "1") Long vehicleId,

        @Schema(description = "Latitud actual", example = "38.1408") Double latitude,

        @Schema(description = "Longitud actual", example = "-0.8844") Double longitude,

        @Schema(description = "Progreso del trayecto (0-100)", example = "45") Integer progress,

        @Schema(description = "Velocidad actual en km/h", example = "87.5") Double speedKmh,

        @Schema(description = "Estado actual del vehículo", example = "EN_RUTA", allowableValues = {
                "IDLE", "EN_RUTA", "PAUSADO", "COMPLETADO", "STOPPED" }) String status,

        @Schema(description = "Última actualización", example = "2026-09-29T14:30:00Z") OffsetDateTime updatedAt){
    public static VehicleStatusDto empty(Long vehicleId) {
        return new VehicleStatusDto(vehicleId, null, null, 0, 0.0, "IDLE", null);
    }
}