package com.ecargohub.backend.dto.vehicle;

import com.ecargohub.backend.domain.enums.AlertSeverityEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AaosNotificationRequest(@NotBlank @Size(max = 100) String title,
        @NotBlank @Size(max = 1000) String message, @NotNull AlertSeverityEnum severity) {
}
