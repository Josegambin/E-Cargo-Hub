package com.ecargohub.backend.service;

import com.ecargohub.backend.dto.command.VehicleCommandDto;
import com.ecargohub.backend.dto.command.VehicleCommandRequest;
import com.ecargohub.backend.entity.VehicleCommandEntity;
import com.ecargohub.backend.entity.VehicleEntity;
import com.ecargohub.backend.exception.ResourceNotFoundException;
import com.ecargohub.backend.mapper.CommandMapper;
import com.ecargohub.backend.repository.VehicleCommandRepository;
import com.ecargohub.backend.repository.VehicleRepository;
import com.ecargohub.backend.interfaces.VehicleCommandService;
import com.ecargohub.backend.kafka.KafkaProducerService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@Transactional
public class VehicleCommandServiceImpl implements VehicleCommandService {

    private final VehicleCommandRepository commandRepository;
    private final VehicleRepository vehicleRepository;
    private final CommandMapper commandMapper;
    private final KafkaProducerService kafkaProducerService;

    public VehicleCommandServiceImpl(VehicleCommandRepository commandRepository,
                                     VehicleRepository vehicleRepository,
                                     CommandMapper commandMapper) {
        this.commandRepository = commandRepository;
        this.vehicleRepository = vehicleRepository;
        this.commandMapper = commandMapper;
        this.kafkaProducerService = null;
    }

    @Override
public VehicleCommandDto sendCommand(Long vehicleId, VehicleCommandRequest request) {
    VehicleEntity vehicle = vehicleRepository.findById(vehicleId)
            .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + vehicleId));

    VehicleCommandEntity entity = new VehicleCommandEntity();
    entity.setVehicle(vehicle);
    entity.setCommand(request.command());
    entity.setValue(request.value());
    entity.setCreatedAt(OffsetDateTime.now());

    VehicleCommandEntity saved = commandRepository.save(entity);
    VehicleCommandDto dto = commandMapper.toDto(saved);

    // Publicar en Kafka
    kafkaProducerService.sendVehicleCommand(dto);

    return dto;
}
}