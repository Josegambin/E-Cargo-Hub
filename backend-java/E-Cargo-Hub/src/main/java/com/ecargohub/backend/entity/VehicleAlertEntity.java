package com.ecargohub.backend.entity;

import com.ecargohub.backend.domain.enums.AlertSeverityEnum;
import com.ecargohub.backend.domain.enums.AlertTypeEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "vehicle_alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VehicleAlertEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AlertTypeEnum type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertSeverityEnum severity;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "current_speed")
    private Double currentSpeed;

    @Column(name = "speed_limit")
    private Double speedLimit;

    @Column(nullable = false)
    private OffsetDateTime timestamp;

    // -------- Relaciones --------

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private VehicleEntity vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "simulation_id")
    private SimulationEntity simulation;
}