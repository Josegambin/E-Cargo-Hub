package com.ecargohub.backend.entity;

import com.ecargohub.backend.domain.VehicleStatus;
import com.ecargohub.backend.domain.VehicleType;
import com.ecargohub.backend.domain.enums.VehicleEnergyTypeEnum;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
public class VehicleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VehicleType type;

    @Column(name = "max_speed", nullable = false)
    private Double maxSpeed;

    @Enumerated(EnumType.STRING)
    @Column(name = "energy_type", length = 24)
    private VehicleEnergyTypeEnum energyType;

    @Column(name = "energy_capacity")
    private Double energyCapacity;

    @Column(name = "energy_consumption_per_100")
    private Double energyConsumptionPer100;

    @Column(name = "remaining_energy")
    private Double remainingEnergy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VehicleStatus status;

    @PostLoad
    @PrePersist
    @PreUpdate
    private void ensureEnergyConfiguration() {
        if (energyType == null) {
            energyType = VehicleEnergyTypeEnum.BATTERY_ELECTRIC;
        }
        if (energyCapacity == null) {
            energyCapacity = energyType == VehicleEnergyTypeEnum.BATTERY_ELECTRIC ? 75.0 : 60.0;
        }
        if (energyConsumptionPer100 == null) {
            energyConsumptionPer100 = energyType == VehicleEnergyTypeEnum.BATTERY_ELECTRIC ? 18.0 : 8.0;
        }
        if (remainingEnergy == null) {
            remainingEnergy = energyCapacity;
        }
    }

    @Transient
    public Double getEnergyPercent() {
        return energyCapacity == null || energyCapacity == 0 || remainingEnergy == null ? 100.0
                : Math.max(0.0, Math.min(100.0, remainingEnergy / energyCapacity * 100.0));
    }

}