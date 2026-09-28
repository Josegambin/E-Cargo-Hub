package com.ecargohub.backend.dto.status;

import java.time.OffsetDateTime;

public record VehicleStatusDto(Long vehicleId, Double latitude, Double longitude, Integer progress, Double speedKmh,
        String status, OffsetDateTime updatedAt) {

    public static VehicleStatusDto empty(Long vehicleId) {
        return new VehicleStatusDto(vehicleId, null, null, 0, 0.0, "IDLE", null);
    }
}