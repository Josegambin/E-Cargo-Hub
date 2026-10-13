package com.ecargohub.backend.service;

import com.ecargohub.backend.domain.VehicleType;
import com.ecargohub.backend.domain.enums.VehicleCommandTypeEnum;
import com.ecargohub.backend.dto.route.RouteResponseDto;
import com.ecargohub.backend.dto.command.VehicleCommandDto;
import com.ecargohub.backend.dto.command.VehicleCommandRequest;
import com.ecargohub.backend.entity.GeoPointEmbeddable;
import com.ecargohub.backend.entity.RouteEntity;
import com.ecargohub.backend.entity.VehicleCommandEntity;
import com.ecargohub.backend.entity.VehicleEntity;
import com.ecargohub.backend.exception.VehicleAlreadyRunningException;
import com.ecargohub.backend.kafka.KafkaProducerService;
import com.ecargohub.backend.mapper.CommandMapper;
import com.ecargohub.backend.repository.RouteRepository;
import com.ecargohub.backend.repository.VehicleCommandRepository;
import com.ecargohub.backend.repository.VehicleRepository;
import com.ecargohub.backend.service.geo.GraphHopperService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.FutureTask;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleCommandServiceImplTest {

    @Mock
    private VehicleCommandRepository commandRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private CommandMapper commandMapper;
    @Mock
    private KafkaProducerService kafkaProducerService;
    @Mock
    private SimulationRegistry simulationRegistry;
    @Mock
    private RouteRepository routeRepository;
    @Mock
    private GraphHopperService graphHopperService;

    private VehicleCommandServiceImpl service;
    private SimulationRegistry.SimulationHandle runningHandle;

    @BeforeEach
    void setUp() {
        service = new VehicleCommandServiceImpl(commandRepository, vehicleRepository, commandMapper,
                kafkaProducerService, simulationRegistry, routeRepository, graphHopperService);
        VehicleEntity vehicle = new VehicleEntity();
        vehicle.setId(1L);
        vehicle.setName("TEST-1");
        vehicle.setType(VehicleType.CAR);
        vehicle.setMaxSpeed(100.0);
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        lenient().when(commandRepository.save(any(VehicleCommandEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(commandMapper.toDto(any(VehicleCommandEntity.class))).thenReturn(
                new VehicleCommandDto(1L, UUID.randomUUID(), 1L, VehicleCommandTypeEnum.SET_SPEED,
                        60.0, null, null, null, null, null));
        runningHandle = new SimulationRegistry.SimulationHandle(1L, new FutureTask<>(() -> {
        }, null));
        lenient().when(simulationRegistry.getControls(1L))
                .thenReturn(new com.ecargohub.backend.dto.vehicle.VehicleControlStateDto(
                        1L, true, false, false, false, "OFF", false, false, 22.0, 0, "AUTO"));
        lenient().when(simulationRegistry.isRunning(1L)).thenReturn(true);
        lenient().when(simulationRegistry.get(1L)).thenReturn(runningHandle);
    }

    @Test
    void setSpeedPersistsTheRequestedTargetForTheSimulation() {
        service.sendCommand(1L, request(VehicleCommandTypeEnum.SET_SPEED, 60.0));

        ArgumentCaptor<VehicleCommandEntity> saved = ArgumentCaptor.forClass(VehicleCommandEntity.class);
        verify(commandRepository).save(saved.capture());
        assertEquals(VehicleCommandTypeEnum.SET_SPEED, saved.getValue().getCommand());
        assertEquals(60.0, saved.getValue().getValue());
        verify(kafkaProducerService).sendVehicleCommand(any());
    }

    @Test
    void increaseSpeedClampsTheTargetToTheVehicleMaximum() {
        runningHandle.setTargetSpeedKmh(95.0);
        service.sendCommand(1L, request(VehicleCommandTypeEnum.INCREASE_SPEED, 10.0));

        ArgumentCaptor<VehicleCommandEntity> saved = ArgumentCaptor.forClass(VehicleCommandEntity.class);
        verify(commandRepository).save(saved.capture());
        assertEquals(100.0, saved.getValue().getValue());
    }

    @Test
    void speedAboveVehicleMaximumIsRejectedBeforePublishing() {
        assertThrows(IllegalArgumentException.class,
                () -> service.sendCommand(1L, request(VehicleCommandTypeEnum.SET_SPEED, 101.0)));

        verify(commandRepository, never()).save(any());
        verify(kafkaProducerService, never()).sendVehicleCommand(any());
    }

    @Test
    void emergencyBrakeIsAcceptedForAnActiveSimulation() {
        service.sendCommand(1L, request(VehicleCommandTypeEnum.EMERGENCY_BRAKE, null));

        ArgumentCaptor<VehicleCommandEntity> saved = ArgumentCaptor.forClass(VehicleCommandEntity.class);
        verify(commandRepository).save(saved.capture());
        assertEquals(VehicleCommandTypeEnum.EMERGENCY_BRAKE, saved.getValue().getCommand());
    }

    @Test
    void startingAnAlreadyRunningSimulationIsRejected() {
        assertThrows(VehicleAlreadyRunningException.class,
                () -> service.sendCommand(1L, request(VehicleCommandTypeEnum.START, null)));

        verify(commandRepository, never()).save(any());
    }

    @Test
    void startingWithIgnitionOffIsRejected() {
        when(simulationRegistry.isRunning(1L)).thenReturn(false);
        when(simulationRegistry.getControls(1L)).thenReturn(
                new com.ecargohub.backend.dto.vehicle.VehicleControlStateDto(
                        1L, false, false, false, false, "OFF", false, false, 22.0, 0, "AUTO"));

        assertThrows(IllegalStateException.class,
                () -> service.sendCommand(1L, startRequest(null)));

        verify(commandRepository, never()).save(any());
        verify(kafkaProducerService, never()).sendVehicleCommand(any());
    }

    @Test
    void startingWithoutRegisteredRouteIsRejected() {
        when(simulationRegistry.isRunning(1L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> service.sendCommand(1L, startRequest(null)));

        verify(routeRepository, never()).findById(any());
        verify(commandRepository, never()).save(any());
    }

    @Test
    void startUsesRegisteredRouteAndRequiresRoadSpeedLimits() {
        when(simulationRegistry.isRunning(1L)).thenReturn(false);
        when(routeRepository.findById(9L)).thenReturn(Optional.of(route(9L)));
        when(graphHopperService.calculateRoute(38.0, -0.9, 38.2, -1.1, 100.0))
                .thenReturn(new RouteResponseDto(
                        java.util.List.of(new double[] { 38.0, -0.9 }, new double[] { 38.2, -1.1 }),
                        1000.0, 100.0, java.util.List.of(50.0)));

        service.sendCommand(1L, startRequest(9L));

        ArgumentCaptor<VehicleCommandEntity> saved = ArgumentCaptor.forClass(VehicleCommandEntity.class);
        verify(commandRepository).save(saved.capture());
        assertEquals(38.0, saved.getValue().getOriginLat());
        assertEquals(-1.1, saved.getValue().getDestLon());
        verify(graphHopperService).calculateRoute(38.0, -0.9, 38.2, -1.1, 100.0);
    }

    @Test
    void startRejectsRouteWithoutCompleteSpeedLimits() {
        when(simulationRegistry.isRunning(1L)).thenReturn(false);
        when(routeRepository.findById(9L)).thenReturn(Optional.of(route(9L)));
        when(graphHopperService.calculateRoute(38.0, -0.9, 38.2, -1.1, 100.0))
                .thenReturn(new RouteResponseDto(
                        java.util.List.of(new double[] { 38.0, -0.9 }, new double[] { 38.2, -1.1 }),
                        1000.0, 100.0, java.util.List.of()));

        assertThrows(IllegalStateException.class,
                () -> service.sendCommand(1L, startRequest(9L)));

        verify(commandRepository, never()).save(any());
        verify(kafkaProducerService, never()).sendVehicleCommand(any());
    }

    @Test
    void controlValuesAreValidatedBeforePublishing() {
        assertThrows(IllegalArgumentException.class,
                () -> service.sendCommand(1L, request(VehicleCommandTypeEnum.SET_HEADLIGHTS, 2.0)));

        verify(commandRepository, never()).save(any());
        verify(kafkaProducerService, never()).sendVehicleCommand(any());
    }

    @Test
    void turnSignalSupportsOffLeftAndRightOnly() {
        service.sendCommand(1L, request(VehicleCommandTypeEnum.SET_TURN_SIGNAL, 2.0));

        ArgumentCaptor<VehicleCommandEntity> saved = ArgumentCaptor.forClass(VehicleCommandEntity.class);
        verify(commandRepository).save(saved.capture());
        assertEquals(2.0, saved.getValue().getValue());
    }

    @Test
    void climateControlsValidateAndPublishValues() {
        service.sendCommand(1L, request(VehicleCommandTypeEnum.SET_CLIMATE_TEMPERATURE, 20.5));
        service.sendCommand(1L, request(VehicleCommandTypeEnum.SET_CLIMATE_FAN_SPEED, 4.0));
        service.sendCommand(1L, request(VehicleCommandTypeEnum.SET_CLIMATE_MODE, 1.0));
        service.sendCommand(1L, request(VehicleCommandTypeEnum.SET_CLIMATE_POWER, 1.0));

        verify(commandRepository, org.mockito.Mockito.times(4)).save(any());
        verify(kafkaProducerService, org.mockito.Mockito.times(4)).sendVehicleCommand(any());
    }

    @Test
    void rejectsOutOfRangeClimateControls() {
        assertThrows(IllegalArgumentException.class,
                () -> service.sendCommand(1L, request(VehicleCommandTypeEnum.SET_CLIMATE_TEMPERATURE, 31.0)));
        assertThrows(IllegalArgumentException.class,
                () -> service.sendCommand(1L, request(VehicleCommandTypeEnum.SET_CLIMATE_FAN_SPEED, 2.5)));
        assertThrows(IllegalArgumentException.class,
                () -> service.sendCommand(1L, request(VehicleCommandTypeEnum.SET_CLIMATE_MODE, 3.0)));

        verify(commandRepository, never()).save(any());
        verify(kafkaProducerService, never()).sendVehicleCommand(any());
    }

    private VehicleCommandRequest request(VehicleCommandTypeEnum command, Double value) {
        return new VehicleCommandRequest(command, value, null, null, null, null);
    }

    private VehicleCommandRequest startRequest(Long routeId) {
        return new VehicleCommandRequest(VehicleCommandTypeEnum.START, null, null, null, null, null, routeId);
    }

    private RouteEntity route(Long id) {
        GeoPointEmbeddable origin = new GeoPointEmbeddable();
        origin.setLatitude(38.0);
        origin.setLongitude(-0.9);
        GeoPointEmbeddable destination = new GeoPointEmbeddable();
        destination.setLatitude(38.2);
        destination.setLongitude(-1.1);
        RouteEntity route = new RouteEntity();
        route.setId(id);
        route.setOrigin(origin);
        route.setDestination(destination);
        return route;
    }
}
