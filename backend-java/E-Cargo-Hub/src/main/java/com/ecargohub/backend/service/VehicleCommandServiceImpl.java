package com.ecargohub.backend.service;

import com.ecargohub.backend.dto.command.VehicleCommandDto;
import com.ecargohub.backend.dto.command.VehicleCommandRequest;
import com.ecargohub.backend.entity.VehicleCommandEntity;
import com.ecargohub.backend.entity.VehicleEntity;
import com.ecargohub.backend.entity.RouteEntity;
import com.ecargohub.backend.exception.ResourceNotFoundException;
import com.ecargohub.backend.exception.VehicleAlreadyRunningException;
import com.ecargohub.backend.interfaces.VehicleCommandService;
import com.ecargohub.backend.kafka.KafkaProducerService;
import com.ecargohub.backend.mapper.CommandMapper;
import com.ecargohub.backend.repository.VehicleCommandRepository;
import com.ecargohub.backend.repository.VehicleRepository;
import com.ecargohub.backend.repository.RouteRepository;
import com.ecargohub.backend.service.geo.GraphHopperService;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class VehicleCommandServiceImpl implements VehicleCommandService {

    private final VehicleCommandRepository commandRepository;
    private final VehicleRepository vehicleRepository;
    private final CommandMapper commandMapper;
    private final KafkaProducerService kafkaProducerService;
    private final SimulationRegistry simulationRegistry; // 👈 añadir al constructor
    private final RouteRepository routeRepository;
    private final GraphHopperService graphHopperService;

    public VehicleCommandServiceImpl(VehicleCommandRepository commandRepository, VehicleRepository vehicleRepository,
            CommandMapper commandMapper, KafkaProducerService kafkaProducerService,
            SimulationRegistry simulationRegistry, RouteRepository routeRepository,
            GraphHopperService graphHopperService) {
        this.commandRepository = commandRepository;
        this.vehicleRepository = vehicleRepository;
        this.commandMapper = commandMapper;
        this.kafkaProducerService = kafkaProducerService;
        this.simulationRegistry = simulationRegistry;
        this.routeRepository = routeRepository;
        this.graphHopperService = graphHopperService;
    }

    @Override
    public VehicleCommandDto sendCommand(Long vehicleId, VehicleCommandRequest request) {
        VehicleEntity vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + vehicleId));
        Double commandValue = request.value();

        // 🔒 VALIDACIÓN POR TIPO DE COMANDO
        switch (request.command()) {
        case START -> {
            if (simulationRegistry.isRunning(vehicleId)) {
                throw new VehicleAlreadyRunningException(vehicleId);
            }
            if (!simulationRegistry.getControls(vehicleId).ignitionOn()) {
                throw new IllegalStateException("Vehicle " + vehicleId + " ignition is off");
            }
        }
        case PAUSE -> {
            if (!simulationRegistry.isRunning(vehicleId)) {
                throw new IllegalStateException("Vehicle " + vehicleId + " is not running");
            }
        }
        case RESUME -> {
            SimulationRegistry.SimulationHandle h = simulationRegistry.get(vehicleId);
            if (h == null || !h.isPaused()) {
                throw new IllegalStateException("Vehicle " + vehicleId + " is not paused");
            }
        }
        case STOP -> {
            if (!simulationRegistry.isRunning(vehicleId)) {
                throw new IllegalStateException("Vehicle " + vehicleId + " is not running");
            }
        }
        case SET_SPEED -> {
            requireRunning(vehicleId);
            if (request.value() == null || !Double.isFinite(request.value()) || request.value() < 0
                    || request.value() > vehicle.getMaxSpeed()) {
                throw new IllegalArgumentException("value must be between 0 and the vehicle max speed");
            }
        }
        case INCREASE_SPEED, DECREASE_SPEED -> {
            requireRunning(vehicleId);
            double step = request.value() == null ? 10.0 : request.value();
            if (!Double.isFinite(step) || step <= 0 || step > vehicle.getMaxSpeed()) {
                throw new IllegalArgumentException("value must be positive and no greater than the vehicle max speed");
            }
            SimulationRegistry.SimulationHandle handle = simulationRegistry.get(vehicleId);
            if (handle == null || handle.isFinished()) {
                throw new IllegalStateException("Vehicle " + vehicleId + " is not running");
            }
            double currentSpeed = handle.getTargetSpeedKmh() == null ? Math.min(80.0, vehicle.getMaxSpeed())
                    : handle.getTargetSpeedKmh();
            commandValue = request.command() == com.ecargohub.backend.domain.enums.VehicleCommandTypeEnum.INCREASE_SPEED
                    ? Math.min(vehicle.getMaxSpeed(), currentSpeed + step) : Math.max(0.0, currentSpeed - step);
        }
        case EMERGENCY_BRAKE -> requireRunning(vehicleId);
        case SET_IGNITION, SET_HEADLIGHTS, SET_HIGH_BEAMS, SET_HAZARD_LIGHTS, SET_WIPERS, SET_CLIMATE_POWER -> {
            if (request.value() == null || (request.value() != 0.0 && request.value() != 1.0)) {
                throw new IllegalArgumentException("value must be 0 or 1 for this control");
            }
        }
        case SET_CLIMATE_TEMPERATURE -> {
            if (request.value() == null || !Double.isFinite(request.value()) || request.value() < 16.0
                    || request.value() > 30.0) {
                throw new IllegalArgumentException("climate temperature must be between 16 and 30 degrees Celsius");
            }
        }
        case SET_CLIMATE_FAN_SPEED -> {
            if (request.value() == null || !Double.isFinite(request.value()) || request.value() < 0
                    || request.value() > 7 || request.value() != Math.rint(request.value())) {
                throw new IllegalArgumentException("climate fan speed must be an integer between 0 and 7");
            }
        }
        case SET_CLIMATE_MODE -> {
            if (request.value() == null
                    || (request.value() != 0.0 && request.value() != 1.0 && request.value() != 2.0)) {
                throw new IllegalArgumentException("climate mode must be 0 (AUTO), 1 (COOL), or 2 (HEAT)");
            }
        }
        case SET_TURN_SIGNAL -> {
            if (request.value() == null
                    || (request.value() != 0.0 && request.value() != 1.0 && request.value() != 2.0)) {
                throw new IllegalArgumentException("value must be 0 (off), 1 (left), or 2 (right)");
            }
        }
        default -> throw new IllegalArgumentException("Unexpected value: " + request.command());
        }

        if (request.command() == com.ecargohub.backend.domain.enums.VehicleCommandTypeEnum.START) {
            Long requestedRouteId = request.routeId();
            if (requestedRouteId == null) {
                throw new IllegalArgumentException("routeId is required to start a vehicle");
            }
            RouteEntity route = routeRepository.findById(requestedRouteId)
                    .orElseThrow(() -> new ResourceNotFoundException("Route not found: " + requestedRouteId));
            if (route.getOrigin() == null || route.getDestination() == null) {
                throw new IllegalArgumentException("The selected route must have an origin and destination");
            }
            request = new VehicleCommandRequest(request.command(), request.value(), route.getOrigin().getLatitude(),
                    route.getOrigin().getLongitude(), route.getDestination().getLatitude(),
                    route.getDestination().getLongitude(), requestedRouteId);
            var routedPath = graphHopperService.calculateRoute(request.originLat(), request.originLon(),
                    request.destLat(), request.destLon(), vehicle.getMaxSpeed());
            if (routedPath.coordinates().size() < 2 || routedPath.speedLimitsKmh() == null
                    || routedPath.speedLimitsKmh().size() != routedPath.coordinates().size() - 1) {
                throw new IllegalStateException("The route has incomplete road speed-limit data");
            }
        }

        // A partir de aquí: guardar en BBDD y publicar a Kafka
        VehicleCommandEntity entity = new VehicleCommandEntity();
        entity.setVehicle(vehicle);
        entity.setCommand(request.command());
        entity.setValue(commandValue);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setOriginLat(request.originLat());
        entity.setOriginLon(request.originLon());
        entity.setDestLat(request.destLat());
        entity.setDestLon(request.destLon());

        VehicleCommandEntity saved = commandRepository.save(entity);
        VehicleCommandDto dto = commandMapper.toDto(saved);

        kafkaProducerService.sendVehicleCommand(dto);

        return dto;
    }

    private void requireRunning(Long vehicleId) {
        if (!simulationRegistry.isRunning(vehicleId)) {
            throw new IllegalStateException("Vehicle " + vehicleId + " is not running");
        }
    }
}