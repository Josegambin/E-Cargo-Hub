package com.ecargohub.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "vehicle_current_status")
@Getter
@Setter
@NoArgsConstructor
public class VehicleCurrentStatusEntity {

    @Id
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

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

}