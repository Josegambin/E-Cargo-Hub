package com.ecargohub.backend.service;

import com.ecargohub.backend.dto.command.VehicleCommandDto;
import com.ecargohub.backend.dto.command.VehicleCommandRequest;
import com.ecargohub.backend.entity.VehicleCommandEntity;
import com.ecargohub.backend.entity.VehicleEntity;
import com.ecargohub.backend.exception.ResourceNotFoundException;
import com.ecargohub.backend.exception.VehicleAlreadyRunningException;
import com.ecargohub.backend.interfaces.VehicleCommandService;
import com.ecargohub.backend.kafka.KafkaProducerService;
import com.ecargohub.backend.mapper.CommandMapper;
import com.ecargohub.backend.repository.VehicleCommandRepository;
import com.ecargohub.backend.repository.VehicleRepository;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class VehicleCommandServiceImpl implements VehicleCommandService {

    private final VehicleCommandRepository commandRepository;
    private final VehicleRepository vehicleRepository;
    private final CommandMapper commandMapper;
    private final KafkaProducerService kafkaProducerService;
    private final SimulationRegistry simulationRegistry; // 👈 añadir al constructor

    public VehicleCommandServiceImpl(VehicleCommandRepository commandRepository, VehicleRepository vehicleRepository,
            CommandMapper commandMapper, KafkaProducerService kafkaProducerService,
            SimulationRegistry simulationRegistry) {
        this.commandRepository = commandRepository;
        this.vehicleRepository = vehicleRepository;
        this.commandMapper = commandMapper;
        this.kafkaProducerService = kafkaProducerService;
        this.simulationRegistry = simulationRegistry;
    }

    @Override
    public VehicleCommandDto sendCommand(Long vehicleId, VehicleCommandRequest request) {
        VehicleEntity vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + vehicleId));

        // 🔒 VALIDACIÓN POR TIPO DE COMANDO
        switch (request.command()) {
        case START -> {
            if (simulationRegistry.isRunning(vehicleId)) {
                throw new VehicleAlreadyRunningException(vehicleId);
            }
        }
        case PAUSE -> {
            if (!simulationRegistry.isRunning(vehicleId)) {
                throw new IllegalStateException("Vehicle " + vehicleId + " is not running");
            }
        }
        case RESUME -> {
            SimulationRegistry.SimulationHandle h = simulationRegistry.get(vehicleId);
            if (h == null || !h.isPaused()) {
                throw new IllegalStateException("Vehicle " + vehicleId + " is not paused");
            }
        }
        case STOP -> {
            if (!simulationRegistry.isRunning(vehicleId)) {
                throw new IllegalStateException("Vehicle " + vehicleId + " is not running");
            }
        }
        default -> throw new IllegalArgumentException("Unexpected value: " + request.command());
        }

        // A partir de aquí: guardar en BBDD y publicar a Kafka
        VehicleCommandEntity entity = new VehicleCommandEntity();
        entity.setVehicle(vehicle);
        entity.setCommand(request.command());
        entity.setValue(request.value());
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setOriginLat(request.originLat());
        entity.setOriginLon(request.originLon());
        entity.setDestLat(request.destLat());
        entity.setDestLon(request.destLon());

        VehicleCommandEntity saved = commandRepository.save(entity);
        VehicleCommandDto dto = commandMapper.toDto(saved);

        kafkaProducerService.sendVehicleCommand(dto);

        return dto;
    }
}