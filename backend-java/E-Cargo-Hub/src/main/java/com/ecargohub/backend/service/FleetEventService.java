package com.ecargohub.backend.service;

import com.ecargohub.backend.domain.enums.FleetEventTypeEnum;
import com.ecargohub.backend.dto.fleet.FleetEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class FleetEventService {

    private static final Logger log = LoggerFactory.getLogger(FleetEventService.class);
    private static final String TOPIC = "/topic/fleet-status";

    private final SimpMessagingTemplate messagingTemplate;

    public FleetEventService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void vehicleStarted(Long vehicleId) {
        emit(FleetEventTypeEnum.VEHICLE_STARTED, vehicleId, "EN_RUTA", "Vehículo " + vehicleId + " ha iniciado ruta");
    }

    public void vehiclePaused(Long vehicleId) {
        emit(FleetEventTypeEnum.VEHICLE_PAUSED, vehicleId, "PAUSADO", "Vehículo " + vehicleId + " ha sido pausado");
    }

    public void vehicleResumed(Long vehicleId) {
        emit(FleetEventTypeEnum.VEHICLE_RESUMED, vehicleId, "EN_RUTA", "Vehículo " + vehicleId + " ha sido reanudado");
    }

    public void vehicleStopped(Long vehicleId) {
        emit(FleetEventTypeEnum.VEHICLE_STOPPED, vehicleId, "STOPPED", "Vehículo " + vehicleId + " ha sido detenido");
    }

    public void vehicleCompleted(Long vehicleId) {
        emit(FleetEventTypeEnum.VEHICLE_COMPLETED, vehicleId, "COMPLETADO",
                "Vehículo " + vehicleId + " ha llegado a destino");
    }

    private void emit(FleetEventTypeEnum type, Long vehicleId, String status, String message) {
        FleetEventDto event = FleetEventDto.of(type, vehicleId, status, message);
        messagingTemplate.convertAndSend(TOPIC, (Object) event);
        log.info("📢 Evento de flota: {} -> {}", type, message);
    }
}