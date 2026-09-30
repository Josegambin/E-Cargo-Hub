package com.ecargohub.backend.dto.telemetry;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Punto de telemetría registrado")
public record TelemetryPointDto(

        @Schema(description = "ID interno", example = "1234") Long id,

        @Schema(description = "ID del vehículo", example = "1") Long vehicleId,

        @Schema(description = "Latitud", example = "38.1408") Double latitude,

        @Schema(description = "Longitud", example = "-0.8844") Double longitude,

        @Schema(description = "Progreso del trayecto (0-100)", example = "45") Integer progress,

        @Schema(description = "Velocidad en km/h", example = "87.5") Double speedKmh,

        @Schema(description = "Estado del vehículo", example = "EN_RUTA") String status,

        @Schema(description = "Momento en que se registró", example = "2026-09-29T14:30:00Z") OffsetDateTime recordedAt) {
}