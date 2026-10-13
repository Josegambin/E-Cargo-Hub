package com.ecargohub.backend.controller;

import com.ecargohub.backend.dto.command.FleetCommandRequest;
import com.ecargohub.backend.dto.command.FleetCommandResultDto;
import com.ecargohub.backend.dto.command.VehicleCommandDto;
import com.ecargohub.backend.dto.command.VehicleCommandRequest;
import com.ecargohub.backend.exception.ResourceNotFoundException;
import com.ecargohub.backend.exception.VehicleAlreadyRunningException;
import com.ecargohub.backend.interfaces.VehicleCommandService;
import com.ecargohub.backend.interfaces.VehicleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/fleet/commands")
public class FleetCommandController {

    private final VehicleService vehicleService;
    private final VehicleCommandService commandService;

    public FleetCommandController(VehicleService vehicleService, VehicleCommandService commandService) {
        this.vehicleService = vehicleService;
        this.commandService = commandService;
    }

    @PostMapping
    public ResponseEntity<FleetCommandResultDto> commandAll(@Valid @RequestBody FleetCommandRequest request) {
        List<VehicleCommandDto> accepted = new ArrayList<>();
        Map<Long, String> rejected = new LinkedHashMap<>();

        vehicleService.findAll().forEach(vehicle -> {
            try {
                accepted.add(commandService.sendCommand(vehicle.id(), new VehicleCommandRequest(request.command(),
                        request.value(), null, null, null, null, request.routeId())));
            } catch (IllegalArgumentException | IllegalStateException | VehicleAlreadyRunningException
                    | ResourceNotFoundException e) {
                rejected.put(vehicle.id(), e.getMessage());
            }
        });

        FleetCommandResultDto result = new FleetCommandResultDto(request.command(), accepted.size() + rejected.size(),
                accepted.size(), List.copyOf(accepted), Map.copyOf(rejected), OffsetDateTime.now());
        return ResponseEntity.status(rejected.isEmpty() ? HttpStatus.ACCEPTED : HttpStatus.MULTI_STATUS).body(result);
    }
}
