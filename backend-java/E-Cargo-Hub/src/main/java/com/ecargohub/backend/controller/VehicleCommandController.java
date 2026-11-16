package com.ecargohub.backend.controller;

import com.ecargohub.backend.dto.command.VehicleCommandDto;
import com.ecargohub.backend.dto.command.VehicleCommandRequest;
import com.ecargohub.backend.dto.vhal.VhalWriteRequest;
import com.ecargohub.backend.interfaces.VehicleCommandService;
import com.ecargohub.backend.mapper.VehicleCommandMapper;
import com.ecargohub.backend.repository.VehicleCommandRepository;
import com.ecargohub.backend.repository.VehicleRepository;
import com.ecargohub.backend.service.VhalBridgeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles/{vehicleId}/commands")
@Tag(name = "Comandos", description = "Control de simulación, velocidad y accesorios virtuales del vehículo")
public class VehicleCommandController {

    private final VehicleCommandService vehicleCommandService;
    private final VehicleCommandRepository commandRepository;
    private final VehicleCommandMapper commandMapper;
    private final VehicleRepository vehicleRepository;
    private final VhalBridgeService vhalBridgeService;

    public VehicleCommandController(VehicleCommandService vehicleCommandService,
            VehicleCommandRepository commandRepository, VehicleCommandMapper commandMapper,
            VehicleRepository vehicleRepository, VhalBridgeService vhalBridgeService) {
        this.vehicleCommandService = vehicleCommandService;
        this.commandRepository = commandRepository;
        this.commandMapper = commandMapper;
        this.vehicleRepository = vehicleRepository;
        this.vhalBridgeService = vhalBridgeService;
    }

    @Operation(summary = "Enviar un comando a un vehículo", description = """
            Envía un comando de control a un vehículo. El comando se guarda en BBDD,
            se publica en Kafka y el consumer lo procesa para arrancar/pausar/reanudar/detener
            la simulación del vehículo.

            **Comandos disponibles:** `START`, `PAUSE`, `RESUME`, `STOP`, controles de velocidad (`SET_SPEED`,
            `INCREASE_SPEED`, `DECREASE_SPEED`, `EMERGENCY_BRAKE`) y controles simulados (`SET_IGNITION`,
            `SET_HEADLIGHTS`, `SET_HIGH_BEAMS`, `SET_HAZARD_LIGHTS`, `SET_TURN_SIGNAL`, `SET_WIPERS`,
            `SET_CLIMATE_POWER`, `SET_CLIMATE_TEMPERATURE`, `SET_CLIMATE_FAN_SPEED`, `SET_CLIMATE_MODE`).
            Los comandos de accesorios solo actualizan el estado virtual; no controlan actuadores AAOS.

            **Coordenadas opcionales:** si se envían `originLat/Lon` y `destLat/Lon`,
            la simulación usa esas coordenadas. Si no, aplica el trayecto por defecto Cox → Murcia.
            """)
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Comando aceptado y publicado en Kafka"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (coordenadas fuera de rango o comando nulo)", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Vehículo no encontrado", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "Conflicto: el comando no es válido para el estado actual del vehículo", content = @Content(schema = @Schema(hidden = true))) })
    @PostMapping
    public ResponseEntity<VehicleCommandDto> dispatchCommand(
            @Parameter(description = "ID del vehículo", example = "1") @PathVariable Long vehicleId,
            @Valid @RequestBody VehicleCommandRequest request) {
        return ResponseEntity.ok(vehicleCommandService.sendCommand(vehicleId, request));
    }

    @Operation(summary = "Obtener el historial de comandos de un vehículo", description = "Devuelve todos los comandos enviados al vehículo, ordenados por fecha descendente.")
    @ApiResponse(responseCode = "200", description = "Historial de comandos")
    @GetMapping
    public ResponseEntity<List<VehicleCommandDto>> getVehicleCommandHistory(
            @Parameter(description = "ID del vehículo", example = "1") @PathVariable Long vehicleId) {
        List<VehicleCommandDto> history = commandRepository.findByVehicleIdOrderByCreatedAtDesc(vehicleId).stream()
                .map(commandMapper::toDto).toList();
        return ResponseEntity.ok(history);
    }

    @Operation(summary = "Inyectar un valor manual en el VHAL", description = """
            Escribe una propiedad en el VHAL del emulador AAOS mediante ADB
            (`cmd car_service inject-vhal-event`). Requiere `ECARGO_VHAL_ENABLED=true`
            y el emulador encendido. Útil para pruebas manuales de speed, fuel, etc.
            """)
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Valor inyectado"),
            @ApiResponse(responseCode = "404", description = "Vehículo no encontrado", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "503", description = "Puente VHAL desactivado", content = @Content(schema = @Schema(hidden = true))) })
    @PostMapping("/vhal-write")
    public ResponseEntity<java.util.Map<String, Object>> writeVhal(
            @Parameter(description = "ID del vehículo", example = "1") @PathVariable Long vehicleId,
            @Valid @RequestBody VhalWriteRequest request) {
        vehicleRepository.findById(vehicleId).orElseThrow(
                () -> new com.ecargohub.backend.exception.ResourceNotFoundException("Vehicle not found: " + vehicleId));
        if (!vhalBridgeService.isEnabled()) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE)
                    .body(java.util.Map.of("vehicleId", vehicleId, "vhalEnabled", false));
        }
        String property = request.property().trim().toUpperCase(java.util.Locale.ROOT);
        com.ecargohub.backend.service.VhalBridgeService.VhalResult result = vhalBridgeService.inject(property,
                request.areaId(), request.value());
        if (!result.ok()) {
            java.util.Map<String, Object> error = new java.util.HashMap<>();
            error.put("vehicleId", vehicleId);
            error.put("property", property);
            error.put("areaId", request.areaId());
            error.put("value", request.value());
            error.put("adb", vhalBridgeService.getAdbPath());
            error.put("serial", vhalBridgeService.getSerial());
            error.put("error", result.output() == null ? "" : result.output());
            return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_GATEWAY).body(error);
        }
        return ResponseEntity.ok(java.util.Map.of("vehicleId", vehicleId, "property", property, "areaId",
                request.areaId(), "value", request.value()));
    }

    @Operation(summary = "Estado del puente VHAL", description = "Muestra si el puente está activo, qué adb/serial usa y el último error de inyección.")
    @ApiResponse(responseCode = "200", description = "Estado del puente")
    @GetMapping("/vhal-status")
    public ResponseEntity<java.util.Map<String, Object>> vhalStatus(
            @Parameter(description = "ID del vehículo", example = "1") @PathVariable Long vehicleId) {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("vehicleId", vehicleId);
        body.put("vhalEnabled", vhalBridgeService.isEnabled());
        body.put("adb", vhalBridgeService.getAdbPath());
        body.put("serial", vhalBridgeService.getSerial());
        body.put("lastError", vhalBridgeService.getLastError());
        body.put("lastOutput", vhalBridgeService.getLastOutput());
        return ResponseEntity.ok(body);
    }
}