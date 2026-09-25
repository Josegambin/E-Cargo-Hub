package com.ecargohub.backend.controller;

import com.ecargohub.backend.domain.enums.AlertSeverityEnum;
import com.ecargohub.backend.dto.alert.VehicleAlertDto;
import com.ecargohub.backend.interfaces.AlertService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public List<VehicleAlertDto> getAlerts(
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) Long simulationId,
            @RequestParam(required = false) AlertSeverityEnum severity) {
        return alertService.findAlerts(vehicleId, simulationId, severity);
    }
}