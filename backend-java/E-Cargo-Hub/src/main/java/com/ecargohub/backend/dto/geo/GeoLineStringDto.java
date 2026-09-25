package com.ecargohub.backend.dto.geo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

@Builder
public record GeoLineStringDto(

        @NotNull
        String type,

        @NotNull
        @Size(min = 2)
        List<List<Double>> coordinates
) {}