package com.ecargohub.backend.entity;

import com.ecargohub.backend.domain.enums.VehicleCommandTypeEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "vehicle_commands")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VehicleCommandEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VehicleCommandTypeEnum command;

    private Double value;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    // -------- Relaciones --------

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private VehicleEntity vehicle;
}