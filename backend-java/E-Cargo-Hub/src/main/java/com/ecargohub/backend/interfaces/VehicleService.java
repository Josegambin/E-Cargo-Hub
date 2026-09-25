package com.ecargohub.backend.interfaces;

import com.ecargohub.backend.dto.vehicle.CreateVehicleRequest;
import com.ecargohub.backend.dto.vehicle.UpdateVehicleRequest;
import com.ecargohub.backend.dto.vehicle.VehicleDto;

import java.util.List;

public interface VehicleService {

    List<VehicleDto> findAll();

    VehicleDto findById(Long vehicleId);

    VehicleDto create(CreateVehicleRequest request);

    VehicleDto update(Long vehicleId, UpdateVehicleRequest request);

    void delete(Long vehicleId);
}