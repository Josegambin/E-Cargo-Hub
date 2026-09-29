package com.ecargohub.backend.controller;

import com.ecargohub.backend.dto.command.VehicleCommandDto;
import com.ecargohub.backend.dto.command.VehicleCommandRequest;
import com.ecargohub.backend.interfaces.VehicleCommandService;
import com.ecargohub.backend.mapper.VehicleCommandMapper;
import com.ecargohub.backend.repository.VehicleCommandRepository;

import jakarta.validation.Valid;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vehicle-commands")
public class VehicleCommandController {

    private final VehicleCommandService vehicleCommandService;
    private final VehicleCommandRepository commandRepository;
    private final VehicleCommandMapper commandMapper;

    public VehicleCommandController(VehicleCommandService vehicleCommandService,
            VehicleCommandRepository commandRepository, VehicleCommandMapper commandMapper) {
        this.vehicleCommandService = vehicleCommandService;
        this.commandRepository = commandRepository;
        this.commandMapper = commandMapper;
    }

    @PostMapping("/vehicle/{vehicleId}")
    public ResponseEntity<VehicleCommandDto> dispatchCommand(@PathVariable Long vehicleId,
            @Valid @RequestBody VehicleCommandRequest request) {

        VehicleCommandDto dto = vehicleCommandService.sendCommand(vehicleId, request);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<VehicleCommandDto>> getVehicleCommandHistory(@PathVariable Long vehicleId) {
        List<VehicleCommandDto> history = commandRepository.findByVehicleIdOrderByCreatedAtDesc(vehicleId).stream()
                .map(commandMapper::toDto).toList();
        return ResponseEntity.ok(history);
    }
}
