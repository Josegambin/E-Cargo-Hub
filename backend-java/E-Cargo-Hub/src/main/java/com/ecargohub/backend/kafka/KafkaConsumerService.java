package com.ecargohub.backend.kafka;

import com.ecargohub.backend.dto.command.VehicleCommandDto;
import com.ecargohub.backend.dto.route.RouteResponseDto;
import com.ecargohub.backend.service.geo.GraphHopperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class KafkaConsumerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerService.class);

    private final GraphHopperService graphHopperService;
    private final SimpMessagingTemplate messagingTemplate;

    public KafkaConsumerService(GraphHopperService graphHopperService, SimpMessagingTemplate messagingTemplate) {
        this.graphHopperService = graphHopperService;
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(
        topics = "vehicle-commands", 
        groupId = "e-cargo-hub-consumers",
        properties = {"spring.json.value.default.type=com.ecargohub.backend.dto.command.VehicleCommandDto"}
    )
    public void listenVehicleCommands(@Payload VehicleCommandDto command) {
        log.info("📥 Evento Kafka - Comando ID {}: Vehículo={}, Acción={}", command.id(), command.vehicleId(), command.command());

        // Escucha el comando "START" mapeado de tu enumerado oficial
        if (command.command() != null && "START".equalsIgnoreCase(command.command().name())) {
            
            // Puntos geográficos: De Cox a tu lugar de trabajo en Murcia
            double originLat = 38.1408;  double originLon = -0.8844;
            double destLat = 37.9922;    double destLon = -1.1307;

            // 1. Obtener la ruta optimizada por carretera desde la API de GraphHopper
            RouteResponseDto route = graphHopperService.calculateRoute(originLat, originLon, destLat, destLon);

            // 2. Ejecución asíncrona de telemetría mediante WebSockets
            CompletableFuture.runAsync(() -> simulateVehicleMovement(command.vehicleId(), route));
        }
    }

    private void simulateVehicleMovement(Long vehicleId, RouteResponseDto route) {
        log.info("🚀 Transmitiendo coordenadas por WS para vehículo {}. Puntos: {}", vehicleId, route.coordinates().size());

        for (int i = 0; i < route.coordinates().size(); i++) {
            double[] coords = route.coordinates().get(i);
            
            Map<String, Object> telemetry = Map.of(
                "vehicleId", vehicleId,
                "latitude", coords[0],
                "longitude", coords[1],
                "progress", Math.round(((double) (i + 1) / route.coordinates().size()) * 100),
                "speedKmh", 90.0,
                "timestamp", System.currentTimeMillis()
            );

            // Emisión directa al broker STOMP /topic/vehicle-status
            messagingTemplate.convertAndSend("/topic/vehicle-status", (Object) telemetry);

            try {
                Thread.sleep(1000); // Demora de 1 segundo para emular la velocidad del camión en el mapa
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        log.info("🏁 Vehículo {} llegó a destino.", vehicleId);
    }
}
