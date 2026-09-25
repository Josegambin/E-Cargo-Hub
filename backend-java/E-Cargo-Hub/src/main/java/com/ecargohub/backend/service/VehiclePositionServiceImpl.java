package com.ecargohub.backend.service;

import com.ecargohub.backend.dto.position.VehiclePositionDto;
import com.ecargohub.backend.entity.VehicleEntity;
import com.ecargohub.backend.entity.VehiclePositionEntity;
import com.ecargohub.backend.exception.ResourceNotFoundException;
import com.ecargohub.backend.mapper.PositionMapper;
import com.ecargohub.backend.repository.VehiclePositionRepository;
import com.ecargohub.backend.repository.VehicleRepository;
import com.ecargohub.backend.websocket.SimulationEventPublisher;
import com.ecargohub.backend.interfaces.VehiclePositionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;

@Service
@Transactional
public class VehiclePositionServiceImpl implements VehiclePositionService {

    private final VehiclePositionRepository positionRepository;
    private final VehicleRepository vehicleRepository;
    private final PositionMapper positionMapper;
    private final SimulationEventPublisher eventPublisher;   // ← CAMPO

    public VehiclePositionServiceImpl(VehiclePositionRepository positionRepository,
                                      VehicleRepository vehicleRepository,
                                      PositionMapper positionMapper) {
        this.positionRepository = positionRepository;
        this.vehicleRepository = vehicleRepository;
        this.positionMapper = positionMapper;
        this.eventPublisher = null;
    }

    @Override
    @Transactional(readOnly = true)
    public VehiclePositionDto findCurrentPosition(Long vehicleId) {
        return positionRepository.findTopByVehicleIdOrderByTimestampDesc(vehicleId)
                .map(positionMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Position not found for vehicle: " + vehicleId));
    }

    @Override
    public VehiclePositionDto save(Long vehicleId, VehiclePositionDto position) {
        VehicleEntity vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vehicle not found: " + vehicleId));

        VehiclePositionEntity entity = new VehiclePositionEntity();
        entity.setVehicle(vehicle);
        entity.setLatitude(position.latitude());
        entity.setLongitude(position.longitude());
        entity.setSpeed(position.speed());
        entity.setSpeedLimit(position.speedLimit());
        entity.setAcceleration(position.acceleration());
        entity.setHeading(position.heading());
        entity.setTimestamp(position.timestamp() != null
                ? position.timestamp()
                : OffsetDateTime.now());

         VehiclePositionDto saved = positionMapper.toDto(positionRepository.save(entity));

        // Emitir por WebSocket
        eventPublisher.publishPosition(vehicleId, saved);

        return saved;
    }
}