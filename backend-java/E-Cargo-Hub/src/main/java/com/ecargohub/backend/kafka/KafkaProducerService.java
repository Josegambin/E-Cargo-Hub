package com.ecargohub.backend.kafka;

import com.ecargohub.backend.dto.command.VehicleCommandDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);
    private static final String TOPIC_COMMANDS = "vehicle-commands";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publica un comando de vehículo en Kafka de forma asíncrona ("fire and forget").
     * El hilo HTTP no se bloquea y retorna inmediatamente a Postman.
     */
    public void sendVehicleCommand(VehicleCommandDto command) {
        log.info("Despachando comando asíncrono a Kafka para vehículo: {}", command.vehicleId());

        Message<VehicleCommandDto> message = MessageBuilder
                .withPayload(command)
                .setHeader(KafkaHeaders.TOPIC, TOPIC_COMMANDS)
                .setHeader(KafkaHeaders.KEY, String.valueOf(command.vehicleId()))
                .setHeader("eventType", "VEHICLE_COMMAND")
                .build();

        // Ejecución asíncrona nativa sin llamadas bloqueantes (.get())
        kafkaTemplate.send(message).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("❌ Kafka en segundo plano reportó error (Revisa los listeners en el puerto 8085): {}", ex.getMessage());
            } else {
                log.info("✅ Mensaje confirmado por Kafka en segundo plano: offset={}", result.getRecordMetadata().offset());
            }
        });
        
        log.info("Hilo HTTP liberado con éxito. Postman ya puede recibir el 200 OK.");
    }
}
