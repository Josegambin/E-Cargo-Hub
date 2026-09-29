package com.ecargohub.backend.dto.command;

import com.ecargohub.backend.domain.enums.VehicleCommandTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Comando de vehículo persistido y publicado en Kafka")
public record VehicleCommandDto(

        @Schema(description = "ID interno en BBDD", example = "1") Long id,

        @Schema(description = "UUID único del comando (idempotencia)") UUID commandId,

        @Schema(description = "ID del vehículo", example = "1") Long vehicleId,

        @Schema(description = "Tipo de comando") VehicleCommandTypeEnum command,

        @Schema(description = "Valor asociado (opcional)") Double value,

        @Schema(description = "Latitud de origen (opcional)") Double originLat,

        @Schema(description = "Longitud de origen (opcional)") Double originLon,

        @Schema(description = "Latitud de destino (opcional)") Double destLat,

        @Schema(description = "Longitud de destino (opcional)") Double destLon,

        @Schema(description = "Fecha de creación (ISO-8601)") OffsetDateTime createdAt) {
}