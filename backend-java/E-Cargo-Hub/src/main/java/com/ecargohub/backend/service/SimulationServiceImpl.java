package com.ecargohub.backend.service;

import com.ecargohub.backend.domain.enums.SimulationStatusEnum;
import com.ecargohub.backend.dto.simulation.CreateSimulationRequest;
import com.ecargohub.backend.dto.simulation.SimulationDto;
import com.ecargohub.backend.entity.RouteEntity;
import com.ecargohub.backend.entity.SimulationEntity;
import com.ecargohub.backend.entity.VehicleEntity;
import com.ecargohub.backend.exception.ResourceNotFoundException;
import com.ecargohub.backend.mapper.SimulationMapper;
import com.ecargohub.backend.repository.RouteRepository;
import com.ecargohub.backend.repository.SimulationRepository;
import com.ecargohub.backend.repository.VehicleRepository;
import com.ecargohub.backend.websocket.SimulationEventPublisher;
import com.ecargohub.backend.interfaces.SimulationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional
public class SimulationServiceImpl implements SimulationService {

    private final SimulationRepository simulationRepository;
    private final VehicleRepository vehicleRepository;
    private final RouteRepository routeRepository;
    private final SimulationMapper simulationMapper;
        private final SimulationEventPublisher eventPublisher;


    public SimulationServiceImpl(SimulationRepository simulationRepository,
                                 VehicleRepository vehicleRepository,
                                 RouteRepository routeRepository,
                                 SimulationMapper simulationMapper) {
        this.simulationRepository = simulationRepository;
        this.vehicleRepository = vehicleRepository;
        this.routeRepository = routeRepository;
        this.simulationMapper = simulationMapper;
        this.eventPublisher = null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SimulationDto> findAll() {
        return simulationRepository.findAll().stream()
                .map(simulationMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SimulationDto findById(Long simulationId) {
        return simulationRepository.findById(simulationId)
                .map(simulationMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Simulation not found: " + simulationId));
    }

    @Override
    public SimulationDto create(CreateSimulationRequest request) {
        VehicleEntity vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vehicle not found: " + request.vehicleId()));
        RouteEntity route = routeRepository.findById(request.routeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Route not found: " + request.routeId()));

        SimulationEntity entity = simulationMapper.toEntity(request, vehicle, route);
        return simulationMapper.toDto(simulationRepository.save(entity));
    }

    @Override
public SimulationDto start(Long simulationId) {
    SimulationEntity entity = getSimulation(simulationId);
    entity.setStatus(SimulationStatusEnum.RUNNING);
    entity.setStartedAt(OffsetDateTime.now());
    SimulationDto dto = simulationMapper.toDto(simulationRepository.save(entity));

    // Emitir por WebSocket
    eventPublisher.publishSimulationStatus(simulationId, dto);

    return dto;
}

    @Override
    public SimulationDto pause(Long simulationId) {
        SimulationEntity entity = getSimulation(simulationId);
        entity.setStatus(SimulationStatusEnum.PAUSED);
        return simulationMapper.toDto(simulationRepository.save(entity));
    }

    @Override
    public SimulationDto resume(Long simulationId) {
        SimulationEntity entity = getSimulation(simulationId);
        entity.setStatus(SimulationStatusEnum.RUNNING);
        return simulationMapper.toDto(simulationRepository.save(entity));
    }

    @Override
    public SimulationDto stop(Long simulationId) {
        SimulationEntity entity = getSimulation(simulationId);
        entity.setStatus(SimulationStatusEnum.STOPPED);
        entity.setFinishedAt(OffsetDateTime.now());
        return simulationMapper.toDto(simulationRepository.save(entity));
    }

    private SimulationEntity getSimulation(Long simulationId) {
        return simulationRepository.findById(simulationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Simulation not found: " + simulationId));
    }
}