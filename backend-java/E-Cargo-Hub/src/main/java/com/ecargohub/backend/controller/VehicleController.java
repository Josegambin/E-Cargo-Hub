package com.ecargohub.backend.controller;

import com.ecargohub.backend.dto.vehicle.CreateVehicleRequest;
import com.ecargohub.backend.dto.vehicle.UpdateVehicleRequest;
import com.ecargohub.backend.dto.vehicle.VehicleDto;
import com.ecargohub.backend.interfaces.VehicleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping
    public List<VehicleDto> getAll() {
        return vehicleService.findAll();
    }

    @GetMapping("/{vehicleId}")
    public VehicleDto getOne(@PathVariable Long vehicleId) {
        return vehicleService.findById(vehicleId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleDto create(@Valid @RequestBody CreateVehicleRequest request) {
        return vehicleService.create(request);
    }

    @PutMapping("/{vehicleId}")
    public VehicleDto update(@PathVariable Long vehicleId,
                             @Valid @RequestBody UpdateVehicleRequest request) {
        return vehicleService.update(vehicleId, request);
    }

    @DeleteMapping("/{vehicleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long vehicleId) {
        vehicleService.delete(vehicleId);
    }
}