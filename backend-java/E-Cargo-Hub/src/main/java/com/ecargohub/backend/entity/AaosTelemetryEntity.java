package com.ecargohub.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "aaos_telemetry", indexes = @Index(name = "idx_aaos_telemetry_vehicle_time", columnList = "vehicle_id, received_at DESC"))
@Getter
@Setter
public class AaosTelemetryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(nullable = false, length = 100)
    private String source;

    @Column(name = "captured_at", nullable = false)
    private OffsetDateTime capturedAt;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    @Column(name = "sensor_values", nullable = false, columnDefinition = "text")
    private String valuesJson;

    @Column(name = "available_properties", nullable = false, columnDefinition = "text")
    private String availablePropertiesJson;

    @Column(name = "read_errors", nullable = false, columnDefinition = "text")
    private String readErrorsJson;

    @PrePersist
    void prePersist() {
        if (receivedAt == null) {
            receivedAt = OffsetDateTime.now();
        }
    }
}
