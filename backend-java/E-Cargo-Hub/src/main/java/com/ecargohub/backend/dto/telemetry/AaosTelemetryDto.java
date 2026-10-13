package com.ecargohub.backend.dto.telemetry;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "Lecturas AAOS recibidas y guardadas")
public record AaosTelemetryDto(Long id, Long vehicleId, String source, OffsetDateTime capturedAt,
        OffsetDateTime receivedAt, Map<String, Object> values, List<String> availableProperties,
        List<String> readErrors) {
}
