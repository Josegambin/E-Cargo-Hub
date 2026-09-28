package com.ecargohub.backend.controller;

import com.ecargohub.backend.dto.status.VehicleStatusDto;
import com.ecargohub.backend.dto.telemetry.TelemetryPointDto;
import com.ecargohub.backend.dto.vehicle.CreateVehicleRequest;
import com.ecargohub.backend.dto.vehicle.UpdateVehicleRequest;
import com.ecargohub.backend.dto.vehicle.VehicleDto;
import com.ecargohub.backend.entity.VehicleEntity;
import com.ecargohub.backend.interfaces.VehicleService;
import com.ecargohub.backend.repository.VehicleRepository;
import com.ecargohub.backend.service.VehicleStatusService;
import com.ecargohub.backend.service.VehicleTelemetryService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;
    private final VehicleStatusService vehicleStatusService;
    private final VehicleRepository vehicleRepository;
    private final VehicleTelemetryService vehicleTelemetryService;

    public VehicleController(VehicleService vehicleService, VehicleStatusService vehicleStatusService,
            VehicleRepository vehicleRepository, VehicleTelemetryService vehicleTelemetryService) {
        this.vehicleService = vehicleService;
        this.vehicleStatusService = vehicleStatusService;
        this.vehicleRepository = vehicleRepository;
        this.vehicleTelemetryService = vehicleTelemetryService;
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

    @GetMapping("/{id}/telemetry")
    public ResponseEntity<List<TelemetryPointDto>> getTelemetry(@PathVariable Long id,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to) {

        if (from != null && to != null) {
            OffsetDateTime fromDt = OffsetDateTime.parse(from);
            OffsetDateTime toDt = OffsetDateTime.parse(to);
            return ResponseEntity.ok(vehicleTelemetryService.getRange(id, fromDt, toDt));
        }
        return ResponseEntity.ok(vehicleTelemetryService.getAll(id));
    }

    @GetMapping("/{id}/telemetry/latest")
    public ResponseEntity<List<TelemetryPointDto>> getLatestTelemetry(@PathVariable Long id,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(vehicleTelemetryService.getLatest(id, Math.min(limit, 500)));
    }

    @DeleteMapping("/{id}/telemetry")
    public ResponseEntity<Map<String, Object>> clearTelemetry(@PathVariable Long id) {
        long deleted = vehicleTelemetryService.deleteAll(id);
        return ResponseEntity.ok(Map.of("vehicleId", id, "deleted", deleted));
    }
}