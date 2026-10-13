package com.ecargohub.backend.service;

import com.ecargohub.backend.domain.VehicleStatus;
import com.ecargohub.backend.domain.enums.VehicleEnergyTypeEnum;
import com.ecargohub.backend.dto.vehicle.CreateVehicleRequest;
import com.ecargohub.backend.dto.vehicle.UpdateVehicleRequest;
import com.ecargohub.backend.dto.vehicle.VehicleDto;
import com.ecargohub.backend.entity.VehicleEntity;
import com.ecargohub.backend.exception.ResourceNotFoundException;
import com.ecargohub.backend.mapper.VehicleMapper;
import com.ecargohub.backend.repository.VehicleRepository;
import com.ecargohub.backend.interfaces.VehicleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;

    public VehicleServiceImpl(VehicleRepository vehicleRepository, VehicleMapper vehicleMapper) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleMapper = vehicleMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleDto> findAll() {
        return vehicleRepository.findAll().stream().map(vehicleMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleDto findById(Long vehicleId) {
        return vehicleRepository.findById(vehicleId).map(vehicleMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + vehicleId));
    }

    @Override
    public VehicleDto create(CreateVehicleRequest request) {
        VehicleEntity entity = vehicleMapper.toEntity(request);
        entity.setStatus(VehicleStatus.IDLE);
        configureEnergy(entity, request.energyType(), request.energyCapacity(), request.energyConsumptionPer100());
        entity.setRemainingEnergy(entity.getEnergyCapacity());
        return vehicleMapper.toDto(vehicleRepository.save(entity));
    }

    @Override
    public VehicleDto update(Long vehicleId, UpdateVehicleRequest request) {
        VehicleEntity entity = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + vehicleId));
        vehicleMapper.updateEntity(entity, request);
        if (request.energyType() != null || request.energyCapacity() != null
                || request.energyConsumptionPer100() != null) {
            configureEnergy(entity, request.energyType(), request.energyCapacity(), request.energyConsumptionPer100());
            if (entity.getRemainingEnergy() == null) {
                entity.setRemainingEnergy(entity.getEnergyCapacity());
            } else {
                entity.setRemainingEnergy(Math.min(entity.getRemainingEnergy(), entity.getEnergyCapacity()));
            }
        }
        if (request.remainingEnergy() != null) {
            if (request.remainingEnergy() > entity.getEnergyCapacity()) {
                throw new IllegalArgumentException("remainingEnergy cannot exceed energyCapacity");
            }
            entity.setRemainingEnergy(request.remainingEnergy());
        }
        return vehicleMapper.toDto(vehicleRepository.save(entity));
    }

    private void configureEnergy(VehicleEntity entity, VehicleEnergyTypeEnum type, Double capacity,
            Double consumptionPer100) {
        VehicleEnergyTypeEnum configuredType = type != null ? type
                : entity.getEnergyType() != null ? entity.getEnergyType() : VehicleEnergyTypeEnum.BATTERY_ELECTRIC;
        entity.setEnergyType(configuredType);
        entity.setEnergyCapacity(capacity != null ? capacity : entity.getEnergyCapacity() != null
                ? entity.getEnergyCapacity() : configuredType == VehicleEnergyTypeEnum.BATTERY_ELECTRIC ? 75.0 : 60.0);
        entity.setEnergyConsumptionPer100(consumptionPer100 != null ? consumptionPer100
                : entity.getEnergyConsumptionPer100() != null ? entity.getEnergyConsumptionPer100()
                        : configuredType == VehicleEnergyTypeEnum.BATTERY_ELECTRIC ? 18.0 : 8.0);
    }

    @Override
    public void delete(Long vehicleId) {
        if (!vehicleRepository.existsById(vehicleId)) {
            throw new ResourceNotFoundException("Vehicle not found: " + vehicleId);
        }
        vehicleRepository.deleteById(vehicleId);
    }
}