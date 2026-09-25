package com.ecargohub.backend.entity;

import com.ecargohub.backend.domain.enums.RouteStatusEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RouteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "latitude",  column = @Column(name = "origin_lat", nullable = false)),
            @AttributeOverride(name = "longitude", column = @Column(name = "origin_lon", nullable = false))
    })
    private GeoPointEmbeddable origin;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "latitude",  column = @Column(name = "dest_lat", nullable = false)),
            @AttributeOverride(name = "longitude", column = @Column(name = "dest_lon", nullable = false))
    })
    private GeoPointEmbeddable destination;

    @Column(name = "distance_meters", nullable = false)
    private Double distanceMeters;

    @Column(name = "duration_seconds", nullable = false)
    private Long durationSeconds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RouteStatusEnum status;

    @Embedded
    private GeoLineStringEmbeddable geometry;
}