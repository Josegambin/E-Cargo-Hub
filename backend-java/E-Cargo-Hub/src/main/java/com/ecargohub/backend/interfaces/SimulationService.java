package com.ecargohub.backend.interfaces;

import com.ecargohub.backend.dto.simulation.CreateSimulationRequest;
import com.ecargohub.backend.dto.simulation.SimulationDto;

import java.util.List;

public interface SimulationService {

    List<SimulationDto> findAll();

    SimulationDto findById(Long simulationId);

    SimulationDto create(CreateSimulationRequest request);

    SimulationDto start(Long simulationId);

    SimulationDto pause(Long simulationId);

    SimulationDto resume(Long simulationId);

    SimulationDto stop(Long simulationId);
}