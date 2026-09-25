package com.ecargohub.backend.domain.enums;

import com.ecargohub.backend.entity.GeoPointEmbeddable;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "warehouses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 250)
    private String address;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "latitude",  column = @Column(name = "location_lat", nullable = false)),
            @AttributeOverride(name = "longitude", column = @Column(name = "location_lon", nullable = false))
    })
    private GeoPointEmbeddable location;
}