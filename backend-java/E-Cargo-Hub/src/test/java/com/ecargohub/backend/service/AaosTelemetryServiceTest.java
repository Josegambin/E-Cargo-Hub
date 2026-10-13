package com.ecargohub.backend.service;

import com.ecargohub.backend.dto.telemetry.AaosTelemetryRequest;
import com.ecargohub.backend.entity.AaosTelemetryEntity;
import com.ecargohub.backend.exception.ResourceNotFoundException;
import com.ecargohub.backend.repository.AaosTelemetryRepository;
import com.ecargohub.backend.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AaosTelemetryServiceTest {

    @Mock
    private AaosTelemetryRepository telemetryRepository;
    @Mock
    private VehicleRepository vehicleRepository;

    @Test
    void savesAndRestoresFlexibleSensorValuesAndReadMetadata() {
        when(vehicleRepository.existsById(7L)).thenReturn(true);
        when(telemetryRepository.save(any(AaosTelemetryEntity.class))).thenAnswer(invocation -> {
            AaosTelemetryEntity entity = invocation.getArgument(0);
            entity.setId(12L);
            return entity;
        });

        AaosTelemetryService service = new AaosTelemetryService(
                telemetryRepository, vehicleRepository, JsonMapper.builder().build());
        AaosTelemetryRequest request = new AaosTelemetryRequest("aaos-emulator",
                OffsetDateTime.parse("2026-10-06T17:30:00Z"),
                Map.of("PERF_VEHICLE_SPEED", 12.5, "SEAT_BELT_BUCKLED@1", true),
                List.of("PERF_VEHICLE_SPEED", "SEAT_BELT_BUCKLED"), List.of());

        var saved = service.save(7L, request);

        assertEquals(12L, saved.id());
        assertEquals(12.5, saved.values().get("PERF_VEHICLE_SPEED"));
        assertEquals(true, saved.values().get("SEAT_BELT_BUCKLED@1"));
        assertEquals(List.of("PERF_VEHICLE_SPEED", "SEAT_BELT_BUCKLED"), saved.availableProperties());

        ArgumentCaptor<AaosTelemetryEntity> entity = ArgumentCaptor.forClass(AaosTelemetryEntity.class);
        verify(telemetryRepository).save(entity.capture());
        assertEquals("aaos-emulator", entity.getValue().getSource());
        verify(vehicleRepository).existsById(7L);
    }

    @Test
    void rejectsTelemetryForUnknownVehicle() {
        when(vehicleRepository.existsById(999L)).thenReturn(false);
        AaosTelemetryService service = new AaosTelemetryService(
                telemetryRepository, vehicleRepository, JsonMapper.builder().build());

        assertThrows(ResourceNotFoundException.class, () -> service.save(999L,
                new AaosTelemetryRequest("aaos-emulator", OffsetDateTime.now(),
                        Map.of("speed", 10), List.of(), List.of())));
        verify(telemetryRepository, never()).save(any());
    }
}
