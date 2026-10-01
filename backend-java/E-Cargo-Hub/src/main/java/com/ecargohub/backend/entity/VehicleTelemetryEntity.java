package com.ecargohub.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "vehicle_telemetry", indexes = {
        @Index(name = "idx_telemetry_vehicle_time", columnList = "vehicle_id, recorded_at DESC") })
@Getter
@Setter
public class VehicleTelemetryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "progress_pct", nullable = false)
    private Integer progress;

    @Column(name = "speed_kmh", nullable = false)
    private Double speedKmh;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    @PrePersist
    void prePersist() {
        if (recordedAt == null)
            recordedAt = OffsetDateTime.now();
    }
}