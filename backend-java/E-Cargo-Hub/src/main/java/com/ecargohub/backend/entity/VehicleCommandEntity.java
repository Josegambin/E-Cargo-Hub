package com.ecargohub.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.ecargohub.backend.domain.enums.VehicleCommandTypeEnum;

@Entity
@Table(name = "vehicle_commands", uniqueConstraints = @UniqueConstraint(name = "uk_command_id", columnNames = "command_id"))
@Getter
@Setter
@NoArgsConstructor
public class VehicleCommandEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "command_id", nullable = false, updatable = false)
    private UUID commandId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private VehicleEntity vehicle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleCommandTypeEnum command;

    @Column(name = "command_value")
    private Double value;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "origin_lat")
    private Double originLat;

    @Column(name = "origin_lon")
    private Double originLon;

    @Column(name = "dest_lat")
    private Double destLat;

    @Column(name = "dest_lon")
    private Double destLon;

    @PrePersist
    void prePersist() {
        if (commandId == null)
            commandId = UUID.randomUUID();
        if (createdAt == null)
            createdAt = OffsetDateTime.now();
    }

    // getters y setters
    public UUID getCommandId() {
        return commandId;
    }

    public void setCommandId(UUID commandId) {
        this.commandId = commandId;
    }
    // ... resto
}