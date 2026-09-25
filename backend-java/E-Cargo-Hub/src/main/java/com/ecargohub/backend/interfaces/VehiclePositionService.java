package com.ecargohub.backend.interfaces;

import com.ecargohub.backend.dto.position.VehiclePositionDto;

public interface VehiclePositionService {

    VehiclePositionDto findCurrentPosition(Long vehicleId);

    VehiclePositionDto save(Long vehicleId, VehiclePositionDto position);
}