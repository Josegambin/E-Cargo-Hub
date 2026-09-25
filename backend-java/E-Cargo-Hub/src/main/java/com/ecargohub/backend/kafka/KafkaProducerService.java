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

    private final KafkaTemplate<Object, Object> kafkaTemplate;

public KafkaProducerService(KafkaTemplate<Object, Object> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
}

    /**
     * Publica un comando de vehículo en Kafka.
     * La clave es el vehicleId para garantizar orden por vehículo.
     */
    public void sendVehicleCommand(VehicleCommandDto command) {
        Message<VehicleCommandDto> message = MessageBuilder
                .withPayload(command)
                .setHeader(KafkaHeaders.TOPIC, TOPIC_COMMANDS)
                .setHeader(KafkaHeaders.KEY, String.valueOf(command.vehicleId()))
                .setHeader("eventType", "VEHICLE_COMMAND")
                .build();

        kafkaTemplate.send(message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Error publicando comando en Kafka: {}", ex.getMessage(), ex);
                    } else {
                        log.info("Comando publicado: vehicleId={}, command={}, offset={}",
                                command.vehicleId(), command.command(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}