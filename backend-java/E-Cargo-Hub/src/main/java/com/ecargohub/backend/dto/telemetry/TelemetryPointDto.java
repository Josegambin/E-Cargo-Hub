package com.ecargohub.backend.dto.telemetry;

import java.time.OffsetDateTime;

public record TelemetryPointDto(Long id, Long vehicleId, Double latitude, Double longitude, Integer progress,
        Double speedKmh, String status, OffsetDateTime recordedAt) {
}