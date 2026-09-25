package com.ecargohub.backend.dto.geo;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record GeoPointDto(

        @NotNull
        @DecimalMin("-90.0") @DecimalMax("90.0")
        Double latitude,

        @NotNull
        @DecimalMin("-180.0") @DecimalMax("180.0")
        Double longitude
) {}