package com.ecargohub.backend.service;

import com.ecargohub.backend.api.model.CreateVehicleRequest;
import com.ecargohub.backend.api.model.UpdateVehicleRequest;
import com.ecargohub.backend.api.model.Vehicle;

import java.util.List;

public interface VehicleService {

    Vehicle create(CreateVehicleRequest request);

    List<Vehicle> findAll();

    Vehicle findById(Long vehicleId);

    Vehicle update(
            Long vehicleId,
            UpdateVehicleRequest request);

    void delete(Long vehicleId);
}