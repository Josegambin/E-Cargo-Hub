package com.ecargohub.backend.websocket;

import com.ecargohub.backend.dto.alert.VehicleAlertDto;
import com.ecargohub.backend.dto.position.VehiclePositionDto;
import com.ecargohub.backend.dto.simulation.SimulationDto;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class SimulationEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public SimulationEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Emite la posición actualizada de un vehículo.
     * Los clientes se suscriben a: /topic/vehicles/{vehicleId}/position
     */
    public void publishPosition(Long vehicleId, VehiclePositionDto position) {
        messagingTemplate.convertAndSend(
                "/topic/vehicles/" + vehicleId + "/position",
                position
        );
    }

    /**
     * Emite una alerta de vehículo.
     * Los clientes se suscriben a: /topic/simulations/{simulationId}/alerts
     * o a /topic/vehicles/{vehicleId}/alerts
     */
    public void publishAlert(VehicleAlertDto alert) {
        // Alerta por vehículo
        messagingTemplate.convertAndSend(
                "/topic/vehicles/" + alert.vehicleId() + "/alerts",
                alert
        );

        // Alerta por simulación (si aplica)
        if (alert.simulationId() != null) {
            messagingTemplate.convertAndSend(
                    "/topic/simulations/" + alert.simulationId() + "/alerts",
                    alert
            );
        }
    }

    /**
     * Emite el cambio de estado de una simulación.
     * Los clientes se suscriben a: /topic/simulations/{simulationId}/status
     */
    public void publishSimulationStatus(Long simulationId, SimulationDto simulation) {
        messagingTemplate.convertAndSend(
                "/topic/simulations/" + simulationId + "/status",
                simulation
        );
    }
}