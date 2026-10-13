package com.ecargohub.backend.dto.device;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VehicleDeviceClaimRequest(@NotBlank @Size(max = 100) String pairingCode,
        @NotBlank @Size(max = 100) String deviceName) {
}
