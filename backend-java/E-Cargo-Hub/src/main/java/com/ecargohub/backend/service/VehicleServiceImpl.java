package com.ecargohub.backend.service;

import com.ecargohub.backend.api.model.CreateVehicleRequest;
import com.ecargohub.backend.api.model.UpdateVehicleRequest;
import com.ecargohub.backend.api.model.Vehicle;
import com.ecargohub.backend.entity.VehicleEntity;
import com.ecargohub.backend.mapper.VehicleMapper;
import com.ecargohub.backend.repository.VehicleRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;

    public VehicleServiceImpl(
            VehicleRepository vehicleRepository,
            VehicleMapper vehicleMapper) {

        this.vehicleRepository = vehicleRepository;
        this.vehicleMapper = vehicleMapper;
    }

    @Override
    public Vehicle create(CreateVehicleRequest request) {

        VehicleEntity entity =
                vehicleMapper.toEntity(request);

        VehicleEntity saved =
                vehicleRepository.save(entity);

        return vehicleMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Vehicle> findAll() {

        return vehicleRepository.findAll()
                .stream()
                .map(vehicleMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Vehicle findById(Long vehicleId) {

        VehicleEntity entity =
                vehicleRepository.findById(vehicleId)
                        .orElseThrow(() ->
                                new VehicleNotFoundException(vehicleId));

        return vehicleMapper.toResponse(entity);
    }

    @Override
    public Vehicle update(
            Long vehicleId,
            UpdateVehicleRequest request) {

        VehicleEntity entity =
                vehicleRepository.findById(vehicleId)
                        .orElseThrow(() ->
                                new VehicleNotFoundException(vehicleId));

        vehicleMapper.updateEntity(entity, request);

        VehicleEntity updated =
                vehicleRepository.save(entity);

        return vehicleMapper.toResponse(updated);
    }

    @Override
    public void delete(Long vehicleId) {

        VehicleEntity entity =
                vehicleRepository.findById(vehicleId)
                        .orElseThrow(() ->
                                new VehicleNotFoundException(vehicleId));

        vehicleRepository.delete(entity);
    }
}