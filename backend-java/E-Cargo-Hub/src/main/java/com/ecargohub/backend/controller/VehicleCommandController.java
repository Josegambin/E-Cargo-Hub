package com.ecargohub.backend.controller;

import com.ecargohub.backend.dto.command.VehicleCommandDto;
import com.ecargohub.backend.entity.VehicleCommandEntity;
import com.ecargohub.backend.entity.VehicleEntity;
import com.ecargohub.backend.kafka.KafkaProducerService;
import com.ecargohub.backend.mapper.VehicleCommandMapper;
import com.ecargohub.backend.repository.VehicleCommandRepository;
import com.ecargohub.backend.repository.VehicleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/vehicle-commands")
public class VehicleCommandController {

    private final KafkaProducerService kafkaProducerService;
    private final VehicleCommandRepository commandRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleCommandMapper commandMapper;

    public VehicleCommandController(KafkaProducerService kafkaProducerService,
                                    VehicleCommandRepository commandRepository,
                                    VehicleRepository vehicleRepository,
                                    VehicleCommandMapper commandMapper) {
        this.kafkaProducerService = kafkaProducerService;
        this.commandRepository = commandRepository;
        this.vehicleRepository = vehicleRepository;
        this.commandMapper = commandMapper;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<String> dispatchCommand(@RequestBody VehicleCommandDto commandDto) {
        
        // 1. Validar y recuperar que el vehículo exista en PostgreSQL (ej: id 101 de Postman)
        VehicleEntity vehicle = vehicleRepository.findById(commandDto.vehicleId())
                .orElseGet(() -> {
                    // Fallback de desarrollo: si no existe el vehículo, creamos uno de pruebas rápido
                    VehicleEntity mockVehicle = new VehicleEntity();
                    mockVehicle.setId(commandDto.vehicleId());
                    return vehicleRepository.save(mockVehicle);
                });

        // 2. Mapear los datos entrantes a la Entidad JPA de base de datos
        VehicleCommandEntity commandEntity = new VehicleCommandEntity();
        commandEntity.setCommand(commandDto.command());
        commandEntity.setValue(commandDto.value());
        commandEntity.setCreatedAt(commandDto.createdAt() != null ? commandDto.createdAt() : OffsetDateTime.now());
        commandEntity.setVehicle(vehicle);

        // 3. Persistir de forma segura en PostgreSQL
        VehicleCommandEntity savedEntity = commandRepository.save(commandEntity);

        // 4. Transformar la entidad guardada de vuelta a DTO (con su ID autogenerado) y enviarlo a Kafka
        VehicleCommandDto enrichedDto = commandMapper.toDto(savedEntity);
        kafkaProducerService.sendVehicleCommand(enrichedDto);

        return ResponseEntity.ok("Comando registrado en base de datos y despachado a Kafka con éxito.");
    }
}
