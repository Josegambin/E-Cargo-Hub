package com.ecargohub.backend.service;

import com.ecargohub.backend.domain.enums.VehicleEnergyTypeEnum;
import com.ecargohub.backend.dto.status.VehicleStatusDto;
import com.ecargohub.backend.entity.VehicleDeviceEnrollmentEntity;
import com.ecargohub.backend.entity.VehicleDeviceEntity;
import com.ecargohub.backend.entity.VehicleEntity;
import com.ecargohub.backend.repository.VehicleDeviceEnrollmentRepository;
import com.ecargohub.backend.repository.VehicleDeviceRepository;
import com.ecargohub.backend.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleDeviceServiceTest {

    @Mock
    private VehicleDeviceEnrollmentRepository enrollmentRepository;
    @Mock
    private VehicleDeviceRepository deviceRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private VehicleStatusService statusService;
    @Mock
    private SimulationRegistry simulationRegistry;

    private VehicleDeviceService service;
    private VehicleEntity vehicle;

    @BeforeEach
    void setUp() {
        service = new VehicleDeviceService(enrollmentRepository, deviceRepository, vehicleRepository, statusService,
                simulationRegistry, "https://display.example/vehicle-display");
        vehicle = new VehicleEntity();
        vehicle.setId(4L);
        vehicle.setEnergyType(VehicleEnergyTypeEnum.BATTERY_ELECTRIC);
        when(vehicleRepository.findById(4L)).thenReturn(Optional.of(vehicle));
        when(enrollmentRepository.save(any(VehicleDeviceEnrollmentEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void enrollmentReturnsAWorkingLocalQrAndStoresOnlyTheCodeHash() {
        var response = service.createEnrollment(4L);

        assertEquals(4L, response.vehicleId());
        assertTrue(response.pairingUrl().startsWith("https://display.example/vehicle-display?pairingCode="));
        assertTrue(response.qrSvg().startsWith("<svg xmlns=\"http://www.w3.org/2000/svg\""));
        assertTrue(response.qrSvg().contains("<path d=\"M"));
        assertTrue(response.expiresAt().isAfter(OffsetDateTime.now()));

        ArgumentCaptor<VehicleDeviceEnrollmentEntity> enrollment = ArgumentCaptor
                .forClass(VehicleDeviceEnrollmentEntity.class);
        verify(enrollmentRepository).save(enrollment.capture());
        assertFalse(enrollment.getValue().getCodeHash().contains(response.pairingCode()));
        long enrollmentLifetime = Duration.between(enrollment.getValue().getCreatedAt(), enrollment.getValue()
                .getExpiresAt()).toSeconds();
        assertTrue(enrollmentLifetime >= 599 && enrollmentLifetime <= 600);
    }

    @Test
    void deviceStateIncludesRouteAndEstimatedDrivingMetrics() {
        vehicle.setEnergyCapacity(75.0);
        vehicle.setEnergyConsumptionPer100(18.0);
        VehicleDeviceEntity device = new VehicleDeviceEntity();
        device.setVehicle(vehicle);
        when(deviceRepository.findByCredentialHashAndEnabledTrue(anyString())).thenReturn(Optional.of(device));
        when(statusService.getStatus(4L)).thenReturn(Optional.of(new VehicleStatusDto(4L, 38.1, -0.4, 25, 60.0,
                50.0, VehicleEnergyTypeEnum.BATTERY_ELECTRIC, "D3", "EN_RUTA", OffsetDateTime.now())));
        when(simulationRegistry.getRouteCoordinates(4L))
                .thenReturn(List.of(new double[] { 38.1, -0.4 }, new double[] { 38.2, -0.5 }));

        var state = service.state("read-only-token");

        assertEquals(2, state.routeCoordinates().size());
        assertEquals(38.2, state.routeCoordinates().get(1)[0]);
        assertEquals(208.33, state.estimatedRangeKm(), 0.01);
        assertEquals(2625, state.estimatedRpm());
    }
}
