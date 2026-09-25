package com.ecargohub.backend.controller;

import com.ecargohub.backend.dto.simulation.CreateSimulationRequest;
import com.ecargohub.backend.dto.simulation.SimulationDto;
import com.ecargohub.backend.interfaces.SimulationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/simulations")
public class SimulationController {

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @GetMapping
    public List<SimulationDto> getAll() {
        return simulationService.findAll();
    }

    @GetMapping("/{simulationId}")
    public SimulationDto getOne(@PathVariable Long simulationId) {
        return simulationService.findById(simulationId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SimulationDto create(@Valid @RequestBody CreateSimulationRequest request) {
        return simulationService.create(request);
    }

    @PostMapping("/{simulationId}/start")
    public SimulationDto start(@PathVariable Long simulationId) {
        return simulationService.start(simulationId);
    }

    @PostMapping("/{simulationId}/pause")
    public SimulationDto pause(@PathVariable Long simulationId) {
        return simulationService.pause(simulationId);
    }

    @PostMapping("/{simulationId}/resume")
    public SimulationDto resume(@PathVariable Long simulationId) {
        return simulationService.resume(simulationId);
    }

    @PostMapping("/{simulationId}/stop")
    public SimulationDto stop(@PathVariable Long simulationId) {
        return simulationService.stop(simulationId);
    }
}