package com.ecargohub.backend.dto.device;

import java.time.OffsetDateTime;

public record VehicleDeviceEnrollmentDto(Long vehicleId, String pairingCode, String pairingUrl, String qrSvg,
        OffsetDateTime expiresAt) {
}
