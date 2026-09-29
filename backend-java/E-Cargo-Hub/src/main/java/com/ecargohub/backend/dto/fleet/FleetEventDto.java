package com.ecargohub.backend.dto.fleet;

import com.ecargohub.backend.domain.enums.FleetEventTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Evento global de flota (para notificaciones y dashboards)")
public record FleetEventDto(

        @Schema(description = "Tipo de evento") FleetEventTypeEnum eventType,

        @Schema(description = "ID del vehículo") Long vehicleId,

        @Schema(description = "Estado nuevo del vehículo") String status,

        @Schema(description = "Mensaje legible para mostrar en UI") String message,

        @Schema(description = "Timestamp en milisegundos") Long timestamp) {

    public static FleetEventDto of(FleetEventTypeEnum type, Long vehicleId, String status, String message) {
        return new FleetEventDto(type, vehicleId, status, message, System.currentTimeMillis());
    }
}