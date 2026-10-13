package com.ecargohub.backend.service;

import com.ecargohub.backend.entity.VehicleEntity;
import com.ecargohub.backend.exception.ResourceNotFoundException;
import com.ecargohub.backend.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleEnergyService {

    private final VehicleRepository vehicleRepository;

    public VehicleEnergyService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional
    public double consumeDistance(Long vehicleId, double distanceMeters) {
        VehicleEntity vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + vehicleId));
        double used = (distanceMeters / 1000.0) * vehicle.getEnergyConsumptionPer100() / 100.0;
        double remaining = Math.max(0.0, vehicle.getRemainingEnergy() - used);
        vehicle.setRemainingEnergy(remaining);
        vehicleRepository.save(vehicle);
        return Math.max(0.0, Math.min(100.0, remaining / vehicle.getEnergyCapacity() * 100.0));
    }

    @Transactional(readOnly = true)
    public double getEnergyPercent(Long vehicleId) {
        VehicleEntity vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + vehicleId));
        return Math.max(0.0, Math.min(100.0, vehicle.getRemainingEnergy() / vehicle.getEnergyCapacity() * 100.0));
    }
}
