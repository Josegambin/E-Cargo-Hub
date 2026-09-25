package com.ecargohub.backend.controller;

import com.ecargohub.backend.dto.command.VehicleCommandDto;
import com.ecargohub.backend.dto.command.VehicleCommandRequest;
import com.ecargohub.backend.interfaces.VehicleCommandService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vehicles/{vehicleId}/commands")
public class VehicleCommandController {

    private final VehicleCommandService commandService;

    public VehicleCommandController(VehicleCommandService commandService) {
        this.commandService = commandService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public VehicleCommandDto send(@PathVariable Long vehicleId,
                                  @Valid @RequestBody VehicleCommandRequest request) {
        return commandService.sendCommand(vehicleId, request);
    }
}