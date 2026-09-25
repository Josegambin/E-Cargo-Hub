package com.ecargohub.backend.interfaces;

import com.ecargohub.backend.dto.command.VehicleCommandDto;
import com.ecargohub.backend.dto.command.VehicleCommandRequest;

public interface VehicleCommandService {

    VehicleCommandDto sendCommand(Long vehicleId, VehicleCommandRequest request);
}