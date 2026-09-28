package com.ecargohub.backend.controller;

import com.ecargohub.backend.dto.status.VehicleStatusDto;
import com.ecargohub.backend.dto.vehicle.CreateVehicleRequest;
import com.ecargohub.backend.dto.vehicle.UpdateVehicleRequest;
import com.ecargohub.backend.dto.vehicle.VehicleDto;
import com.ecargohub.backend.entity.VehicleEntity;
import com.ecargohub.backend.interfaces.VehicleService;
import com.ecargohub.backend.repository.VehicleRepository;
import com.ecargohub.backend.service.VehicleStatusService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;
    private final VehicleStatusService vehicleStatusService;
    private final VehicleRepository vehicleRepository;

    public VehicleController(VehicleService vehicleService, VehicleStatusService vehicleStatusService,
            VehicleRepository vehicleRepository) {
        this.vehicleService = vehicleService;
        this.vehicleStatusService = vehicleStatusService;
        this.vehicleRepository = vehicleRepository;
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
    public VehicleDto update(@PathVariable Long vehicleId, @Valid @RequestBody UpdateVehicleRequest request) {
        return vehicleService.update(vehicleId, request);
    }

    @DeleteMapping("/{vehicleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long vehicleId) {
        vehicleService.delete(vehicleId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleEntity> getById(@PathVariable Long id) {
        return vehicleRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<VehicleStatusDto> getStatus(@PathVariable Long id) {
        return vehicleStatusService.getStatus(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(VehicleStatusDto.empty(id)));
    }
}