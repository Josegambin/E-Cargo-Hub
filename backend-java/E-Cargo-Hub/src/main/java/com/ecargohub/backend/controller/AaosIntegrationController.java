package com.ecargohub.backend.controller;

import com.ecargohub.backend.dto.command.VehicleCommandDto;
import com.ecargohub.backend.dto.command.VehicleCommandRequest;
import com.ecargohub.backend.dto.telemetry.AaosTelemetryDto;
import com.ecargohub.backend.dto.telemetry.AaosTelemetryRequest;
import com.ecargohub.backend.dto.vehicle.AaosNotificationDto;
import com.ecargohub.backend.dto.vehicle.AaosNotificationRequest;
import com.ecargohub.backend.dto.vehicle.VehicleControlStateDto;
import com.ecargohub.backend.exception.ResourceNotFoundException;
import com.ecargohub.backend.interfaces.VehicleCommandService;
import com.ecargohub.backend.repository.VehicleRepository;
import com.ecargohub.backend.service.AaosTelemetryService;
import com.ecargohub.backend.service.SimulationRegistry;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles/{vehicleId}/aaos")
public class AaosIntegrationController {

    private final AaosTelemetryService telemetryService;
    private final VehicleRepository vehicleRepository;
    private final VehicleCommandService commandService;
    private final SimpMessagingTemplate messagingTemplate;
    private final SimulationRegistry simulationRegistry;

    public AaosIntegrationController(AaosTelemetryService telemetryService, VehicleRepository vehicleRepository,
            VehicleCommandService commandService, SimpMessagingTemplate messagingTemplate,
            SimulationRegistry simulationRegistry) {
        this.telemetryService = telemetryService;
        this.vehicleRepository = vehicleRepository;
        this.commandService = commandService;
        this.messagingTemplate = messagingTemplate;
        this.simulationRegistry = simulationRegistry;
    }

    @PostMapping("/telemetry")
    @ResponseStatus(HttpStatus.CREATED)
    public AaosTelemetryDto receiveTelemetry(@PathVariable Long vehicleId,
            @Valid @RequestBody AaosTelemetryRequest request) {
        AaosTelemetryDto snapshot = telemetryService.save(vehicleId, request);
        messagingTemplate.convertAndSend("/topic/vehicles/" + vehicleId + "/aaos/telemetry", snapshot);
        return snapshot;
    }

    @GetMapping("/telemetry/latest")
    public List<AaosTelemetryDto> latestTelemetry(@PathVariable Long vehicleId,
            @RequestParam(defaultValue = "1") int limit) {
        return telemetryService.latest(vehicleId, limit);
    }

    @GetMapping("/control-state")
    public VehicleControlStateDto controlState(@PathVariable Long vehicleId) {
        requireVehicle(vehicleId);
        return simulationRegistry.getControls(vehicleId);
    }

    @PostMapping("/notifications")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AaosNotificationDto sendNotification(@PathVariable Long vehicleId,
            @Valid @RequestBody AaosNotificationRequest request) {
        requireVehicle(vehicleId);
        AaosNotificationDto notification = new AaosNotificationDto(vehicleId, request.title(), request.message(),
                request.severity(), OffsetDateTime.now());
        messagingTemplate.convertAndSend("/topic/vehicles/" + vehicleId + "/aaos/notifications", notification);
        return notification;
    }

    @PostMapping("/simulated-commands")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public VehicleCommandDto sendSimulatedCommand(@PathVariable Long vehicleId,
            @Valid @RequestBody VehicleCommandRequest request) {
        requireVehicle(vehicleId);
        VehicleCommandDto command = commandService.sendCommand(vehicleId, request);
        messagingTemplate.convertAndSend("/topic/vehicles/" + vehicleId + "/commands",
                new com.ecargohub.backend.dto.vehicle.AaosVehicleCommandDto(command.commandId(), vehicleId,
                        command.command(), command.value(), "SIMULATOR_ONLY", command.createdAt()));
        return command;
    }

    private void requireVehicle(Long vehicleId) {
        if (!vehicleRepository.existsById(vehicleId)) {
            throw new ResourceNotFoundException("Vehicle not found: " + vehicleId);
        }
    }
}
