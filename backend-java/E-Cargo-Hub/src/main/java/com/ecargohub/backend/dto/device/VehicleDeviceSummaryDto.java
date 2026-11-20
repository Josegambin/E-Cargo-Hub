package com.ecargohub.backend.dto.device;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VehicleDeviceSummaryDto(UUID deviceId, Long vehicleId, String name, boolean enabled,
        OffsetDateTime createdAt) {
}
