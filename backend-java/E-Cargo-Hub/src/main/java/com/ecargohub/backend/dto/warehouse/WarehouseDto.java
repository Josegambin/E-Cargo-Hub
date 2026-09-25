package com.ecargohub.backend.dto.warehouse;

import com.ecargohub.backend.dto.geo.GeoPointDto;
import lombok.Builder;

@Builder
public record WarehouseDto(
        Long id,
        String name,
        String address,
        GeoPointDto location
) {}