package com.ecargohub.backend.service;

import com.ecargohub.backend.domain.enums.AlertSeverityEnum;
import com.ecargohub.backend.dto.alert.VehicleAlertDto;
import com.ecargohub.backend.entity.VehicleAlertEntity;
import com.ecargohub.backend.exception.ResourceNotFoundException;
import com.ecargohub.backend.mapper.AlertMapper;
import com.ecargohub.backend.repository.SimulationRepository;
import com.ecargohub.backend.repository.VehicleAlertRepository;
import com.ecargohub.backend.repository.VehicleRepository;
import com.ecargohub.backend.websocket.SimulationEventPublisher;
import com.ecargohub.backend.interfaces.AlertService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AlertServiceImpl implements AlertService {

    private final VehicleAlertRepository alertRepository;
    private final VehicleRepository vehicleRepository;
    private final SimulationRepository simulationRepository;
    private final AlertMapper alertMapper;
    private final SimulationEventPublisher eventPublisher;   // ← CAMPO


    public AlertServiceImpl(VehicleAlertRepository alertRepository,
                            VehicleRepository vehicleRepository,
                            SimulationRepository simulationRepository,
                            AlertMapper alertMapper) {
        this.alertRepository = alertRepository;
        this.vehicleRepository = vehicleRepository;
        this.simulationRepository = simulationRepository;
        this.alertMapper = alertMapper;
        this.eventPublisher = null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleAlertDto> findAlerts(Long vehicleId, Long simulationId,
                                            AlertSeverityEnum severity) {
        List<VehicleAlertEntity> result;

        if (vehicleId != null && simulationId != null && severity != null) {
            result = alertRepository.findByVehicleIdAndSimulationIdAndSeverity(
                    vehicleId, simulationId, severity);
        } else if (vehicleId != null) {
            result = alertRepository.findByVehicleId(vehicleId);
        } else if (simulationId != null) {
            result = alertRepository.findBySimulationId(simulationId);
        } else if (severity != null) {
            result = alertRepository.findBySeverity(severity);
        } else {
            result = alertRepository.findAll();
        }

        return result.stream().map(alertMapper::toDto).toList();
    }

    @Override
    public VehicleAlertDto create(VehicleAlertDto alert) {
        VehicleAlertEntity entity = new VehicleAlertEntity();

        entity.setVehicle(vehicleRepository.findById(alert.vehicleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vehicle not found: " + alert.vehicleId())));

        if (alert.simulationId() != null) {
            entity.setSimulation(simulationRepository.findById(alert.simulationId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Simulation not found: " + alert.simulationId())));
        }

        entity.setType(alert.type());
        entity.setSeverity(alert.severity());
        entity.setMessage(alert.message());
        entity.setCurrentSpeed(alert.currentSpeed());
        entity.setSpeedLimit(alert.speedLimit());
        entity.setTimestamp(alert.timestamp());

         VehicleAlertDto saved = alertMapper.toDto(alertRepository.save(entity));

        // Emitir por WebSocket
        eventPublisher.publishAlert(saved);

        return saved;
    }
}