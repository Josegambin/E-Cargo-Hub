package com.ecargohub.backend.dto.device;

import java.util.UUID;

public record VehicleDeviceClaimDto(UUID deviceId, Long vehicleId, String deviceToken) {
}
