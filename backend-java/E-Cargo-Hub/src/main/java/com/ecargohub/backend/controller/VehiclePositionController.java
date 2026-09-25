package com.ecargohub.backend.controller;

import com.ecargohub.backend.dto.position.VehiclePositionDto;
import com.ecargohub.backend.interfaces.VehiclePositionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vehicles/{vehicleId}/position")
public class VehiclePositionController {

    private final VehiclePositionService positionService;

    public VehiclePositionController(VehiclePositionService positionService) {
        this.positionService = positionService;
    }

    @GetMapping
    public VehiclePositionDto getCurrent(@PathVariable Long vehicleId) {
        return positionService.findCurrentPosition(vehicleId);
    }
}