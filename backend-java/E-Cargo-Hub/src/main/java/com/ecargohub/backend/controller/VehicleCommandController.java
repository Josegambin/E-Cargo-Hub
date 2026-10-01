package com.ecargohub.backend.controller;

import com.ecargohub.backend.dto.command.VehicleCommandDto;
import com.ecargohub.backend.dto.command.VehicleCommandRequest;
import com.ecargohub.backend.interfaces.VehicleCommandService;
import com.ecargohub.backend.mapper.VehicleCommandMapper;
import com.ecargohub.backend.repository.VehicleCommandRepository;

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
@Tag(name = "Comandos", description = "Envío de comandos a vehículos (START, PAUSE, RESUME, STOP)")
public class VehicleCommandController {

    private final VehicleCommandService vehicleCommandService;
    private final VehicleCommandRepository commandRepository;
    private final VehicleCommandMapper commandMapper;

    public VehicleCommandController(VehicleCommandService vehicleCommandService,
            VehicleCommandRepository commandRepository, VehicleCommandMapper commandMapper) {
        this.vehicleCommandService = vehicleCommandService;
        this.commandRepository = commandRepository;
        this.commandMapper = commandMapper;
    }

    @Operation(summary = "Enviar un comando a un vehículo", description = """
            Envía un comando de control a un vehículo. El comando se guarda en BBDD,
            se publica en Kafka y el consumer lo procesa para arrancar/pausar/reanudar/detener
            la simulación del vehículo.

            **Comandos disponibles:**
            - `START` → arranca la simulación (rechazado con 409 si ya está corriendo)
            - `PAUSE` → pausa la simulación (rechazado con 409 si no está corriendo)
            - `RESUME` → reanuda (rechazado con 409 si no está pausado)
            - `STOP` → detiene la simulación (rechazado con 409 si no está corriendo)

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
}