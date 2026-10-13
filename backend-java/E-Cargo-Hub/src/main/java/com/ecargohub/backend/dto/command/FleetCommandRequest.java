package com.ecargohub.backend.dto.command;

import com.ecargohub.backend.domain.enums.VehicleCommandTypeEnum;
import jakarta.validation.constraints.NotNull;

public record FleetCommandRequest(@NotNull VehicleCommandTypeEnum command, Double value, Long routeId) {
}
