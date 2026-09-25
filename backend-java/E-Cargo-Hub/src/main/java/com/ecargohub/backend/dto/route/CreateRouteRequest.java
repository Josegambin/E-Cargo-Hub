package com.ecargohub.backend.dto.route;

import com.ecargohub.backend.dto.geo.GeoPointDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;

@Builder
public record CreateRouteRequest(

        @NotBlank
        @Size(min = 1, max = 150)
        String name,

        @NotNull
        @Valid
        GeoPointDto origin,

        @NotNull
        @Valid
        GeoPointDto destination
) {}