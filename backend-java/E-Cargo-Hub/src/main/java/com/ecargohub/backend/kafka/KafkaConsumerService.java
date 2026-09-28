package com.ecargohub.backend.kafka;

import com.ecargohub.backend.dto.command.VehicleCommandDto;
import com.ecargohub.backend.service.IdempotencyService;
import com.ecargohub.backend.service.geo.GraphHopperService;
import tools.jackson.databind.json.JsonMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerService.class);

    private final GraphHopperService graphHopperService;
    private final SimpMessagingTemplate messagingTemplate;
    private final JsonMapper objectMapper;
    private final IdempotencyService idempotencyService;

    public KafkaConsumerService(GraphHopperService graphHopperService, SimpMessagingTemplate messagingTemplate,
            IdempotencyService idempotencyService) {
        this.graphHopperService = graphHopperService;
        this.messagingTemplate = messagingTemplate;
        this.idempotencyService = idempotencyService;
        this.objectMapper = JsonMapper.builder().build();
    }

    @KafkaListener(topics = "vehicle-commands", groupId = "e-cargo-hub-consumers", containerFactory = "stringKafkaListenerContainerFactory" // Vinculación
                                                                                                                                            // a
                                                                                                                                            // tu
                                                                                                                                            // factoría
                                                                                                                                            // corregida
    )
    public void listenVehicleCommands(String messageString) {
        log.info("📥 ¡EVENTO DETECTADO EN KAFKA!: {}", messageString);

        try {
            VehicleCommandDto command = objectMapper.readValue(messageString, VehicleCommandDto.class);

            // 🔒 IDEMPOTENCIA: si ya lo procesamos, salimos
            if (!idempotencyService.tryMarkAsProcessed(command.commandId())) {
                return;
            }

            log.info("📦 Mapeado a DTO -> Comando: {}, Vehículo: {}, commandId: {}", command.command(),
                    command.vehicleId(), command.commandId());

            if (command.command() != null && "START".equalsIgnoreCase(command.command().name())) {
                // ... lógica existente de simulación ...
            }

        } catch (Exception e) {
            log.error("❌ Error procesando el evento: {}", e.getMessage(), e);
            // ⚠️ Re-lanzamos para que Kafka NO commitee el offset y reintente
            throw new RuntimeException("Error procesando comando de Kafka", e);
        }
    }
}
