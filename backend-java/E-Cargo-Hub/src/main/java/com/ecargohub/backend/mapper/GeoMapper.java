package com.ecargohub.backend.mapper;

import com.ecargohub.backend.dto.geo.GeoLineStringDto;
import com.ecargohub.backend.dto.geo.GeoPointDto;
import com.ecargohub.backend.entity.GeoLineStringEmbeddable;
import com.ecargohub.backend.entity.GeoPointEmbeddable;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GeoMapper {

    GeoPointDto toDto(GeoPointEmbeddable entity);
    GeoPointEmbeddable toEntity(GeoPointDto dto);

    GeoLineStringDto toDto(GeoLineStringEmbeddable entity);
    GeoLineStringEmbeddable toEntity(GeoLineStringDto dto);
}