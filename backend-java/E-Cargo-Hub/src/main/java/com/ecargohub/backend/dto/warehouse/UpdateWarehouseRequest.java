package com.ecargohub.backend.dto.warehouse;

import com.ecargohub.backend.dto.geo.GeoPointDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;

@Builder
public record UpdateWarehouseRequest(

        @NotBlank
        @Size(min = 1, max = 150)
        String name,

        @Size(max = 250)
        String address,

        @NotNull
        @Valid
        GeoPointDto location
) {}