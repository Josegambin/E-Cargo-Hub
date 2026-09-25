package com.ecargohub.backend.controller;

import com.ecargohub.backend.api.VehiclesApi;
import com.ecargohub.backend.api.model.CreateVehicleRequest;
import com.ecargohub.backend.api.model.UpdateVehicleRequest;
import com.ecargohub.backend.api.model.Vehicle;
import com.ecargohub.backend.service.VehicleService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
public class VehicleController implements VehiclesApi {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @Override
    public ResponseEntity<Vehicle> createVehicle(
            CreateVehicleRequest createVehicleRequest) {

        Vehicle vehicle =
                vehicleService.create(createVehicleRequest);

        return ResponseEntity
                .created(
                        URI.create(
                                "/api/vehicles/"
                                        + vehicle.getId()))
                .body(vehicle);
    }

    @Override
    public ResponseEntity<List<Vehicle>> getVehicles() {

        return ResponseEntity.ok(
                vehicleService.findAll());
    }

    @Override
    public ResponseEntity<Vehicle> getVehicle(
            Long vehicleId) {

        return ResponseEntity.ok(
                vehicleService.findById(vehicleId));
    }

    @Override
    public ResponseEntity<Vehicle> updateVehicle(
            Long vehicleId,
            UpdateVehicleRequest updateVehicleRequest) {

        return ResponseEntity.ok(
                vehicleService.update(
                        vehicleId,
                        updateVehicleRequest));
    }

    @Override
    public ResponseEntity<Void> deleteVehicle(
            Long vehicleId) {

        vehicleService.delete(vehicleId);

        return ResponseEntity.noContent().build();
    }
}