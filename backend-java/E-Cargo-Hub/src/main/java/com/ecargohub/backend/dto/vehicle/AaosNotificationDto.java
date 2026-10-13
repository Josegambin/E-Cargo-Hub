package com.ecargohub.backend.dto.vehicle;

import com.ecargohub.backend.domain.enums.AlertSeverityEnum;

import java.time.OffsetDateTime;

public record AaosNotificationDto(Long vehicleId, String title, String message, AlertSeverityEnum severity,
        OffsetDateTime createdAt) {
}
