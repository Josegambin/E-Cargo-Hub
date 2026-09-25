package com.ecargohub.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "vehicle_positions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VehiclePositionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Double speed;

    @Column(name = "speed_limit")
    private Double speedLimit;

    private Double acceleration;

    @Column(nullable = false)
    private Double heading;

    @Column(nullable = false)
    private OffsetDateTime timestamp;

    // -------- Relaciones --------

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private VehicleEntity vehicle;
}