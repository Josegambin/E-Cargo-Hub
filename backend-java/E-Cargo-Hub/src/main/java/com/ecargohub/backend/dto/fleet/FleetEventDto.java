package com.ecargohub.backend.dto.fleet;

import com.ecargohub.backend.domain.enums.FleetEventTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Evento global de flota (emitido por WebSocket a /topic/fleet-status)")
public record FleetEventDto(

        @Schema(description = "Tipo de evento", example = "VEHICLE_STARTED") FleetEventTypeEnum eventType,

        @Schema(description = "ID del vehículo", example = "1") Long vehicleId,

        @Schema(description = "Estado nuevo", example = "EN_RUTA") String status,

        @Schema(description = "Mensaje legible", example = "Vehículo 1 ha iniciado ruta") String message,

        @Schema(description = "Timestamp en ms", example = "1790756789000") Long timestamp) {
    public static FleetEventDto of(FleetEventTypeEnum type, Long vehicleId, String status, String message) {
        return new FleetEventDto(type, vehicleId, status, message, System.currentTimeMillis());
    }
}