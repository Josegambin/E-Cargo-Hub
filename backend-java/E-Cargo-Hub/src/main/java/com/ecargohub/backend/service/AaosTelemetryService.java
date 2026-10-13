package com.ecargohub.backend.service;

import com.ecargohub.backend.dto.telemetry.AaosTelemetryDto;
import com.ecargohub.backend.dto.telemetry.AaosTelemetryRequest;
import com.ecargohub.backend.entity.AaosTelemetryEntity;
import com.ecargohub.backend.exception.ResourceNotFoundException;
import com.ecargohub.backend.repository.AaosTelemetryRepository;
import com.ecargohub.backend.repository.VehicleRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AaosTelemetryService {

    private final AaosTelemetryRepository telemetryRepository;
    private final VehicleRepository vehicleRepository;
    private final JsonMapper jsonMapper;

    public AaosTelemetryService(AaosTelemetryRepository telemetryRepository, VehicleRepository vehicleRepository,
            JsonMapper jsonMapper) {
        this.telemetryRepository = telemetryRepository;
        this.vehicleRepository = vehicleRepository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public AaosTelemetryDto save(Long vehicleId, AaosTelemetryRequest request) {
        if (!vehicleRepository.existsById(vehicleId)) {
            throw new ResourceNotFoundException("Vehicle not found: " + vehicleId);
        }

        AaosTelemetryEntity entity = new AaosTelemetryEntity();
        entity.setVehicleId(vehicleId);
        entity.setSource(request.source());
        entity.setCapturedAt(request.capturedAt());
        entity.setReceivedAt(OffsetDateTime.now());
        entity.setValuesJson(writeJson(request.values()));
        entity.setAvailablePropertiesJson(
                writeJson(request.availableProperties() == null ? List.of() : request.availableProperties()));
        entity.setReadErrorsJson(writeJson(request.readErrors() == null ? List.of() : request.readErrors()));
        return toDto(telemetryRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<AaosTelemetryDto> latest(Long vehicleId, int limit) {
        if (!vehicleRepository.existsById(vehicleId)) {
            throw new ResourceNotFoundException("Vehicle not found: " + vehicleId);
        }
        if (limit < 1 || limit > 500) {
            throw new IllegalArgumentException("'limit' must be between 1 and 500");
        }
        return telemetryRepository.findByVehicleIdOrderByReceivedAtDesc(vehicleId, PageRequest.of(0, limit)).stream()
                .map(this::toDto).toList();
    }

    private AaosTelemetryDto toDto(AaosTelemetryEntity entity) {
        return new AaosTelemetryDto(entity.getId(), entity.getVehicleId(), entity.getSource(), entity.getCapturedAt(),
                entity.getReceivedAt(), readMap(entity.getValuesJson()), readList(entity.getAvailablePropertiesJson()),
                readList(entity.getReadErrorsJson()));
    }

    private String writeJson(Object value) {
        try {
            return jsonMapper.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("AAOS telemetry contains a value that cannot be serialized", e);
        }
    }

    private Map<String, Object> readMap(String value) {
        try {
            return jsonMapper.readValue(value,
                    jsonMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
        } catch (JacksonException e) {
            throw new IllegalStateException("Stored AAOS telemetry is invalid JSON", e);
        }
    }

    private List<String> readList(String value) {
        try {
            return jsonMapper.readValue(value,
                    jsonMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JacksonException e) {
            throw new IllegalStateException("Stored AAOS telemetry metadata is invalid JSON", e);
        }
    }
}
