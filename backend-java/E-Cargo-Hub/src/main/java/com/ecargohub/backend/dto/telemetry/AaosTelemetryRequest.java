package com.ecargohub.backend.dto.telemetry;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "Lecturas disponibles de un vehículo Android Automotive")
public record AaosTelemetryRequest(

        @NotBlank @Size(max = 100) String source,

        @NotNull OffsetDateTime capturedAt,

        @NotEmpty @Size(max = 200) Map<String, Object> values,

        @Size(max = 200) List<@Size(max = 200) String> availableProperties,

        @Size(max = 50) List<@Size(max = 500) String> readErrors) {
}
