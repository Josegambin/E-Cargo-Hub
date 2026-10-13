package com.ecargohub.backend.dto.command;

import com.ecargohub.backend.domain.enums.VehicleCommandTypeEnum;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record FleetCommandResultDto(VehicleCommandTypeEnum command, int requested, int accepted,
        List<VehicleCommandDto> commands, Map<Long, String> rejected, OffsetDateTime createdAt) {
}
