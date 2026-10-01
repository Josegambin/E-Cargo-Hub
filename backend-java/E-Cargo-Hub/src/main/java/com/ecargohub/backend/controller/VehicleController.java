package com.ecargohub.backend.controller;

import com.ecargohub.backend.dto.status.VehicleStatusDto;
import com.ecargohub.backend.dto.telemetry.TelemetryPointDto;
import com.ecargohub.backend.dto.vehicle.CreateVehicleRequest;
import com.ecargohub.backend.dto.vehicle.UpdateVehicleRequest;
import com.ecargohub.backend.dto.vehicle.VehicleDto;
import com.ecargohub.backend.interfaces.VehicleService;
import com.ecargohub.backend.service.VehicleStatusService;
import com.ecargohub.backend.service.VehicleTelemetryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/vehicles")
@Tag(name = "Vehículos", description = "Gestión de vehículos, estado actual e histórico de telemetría")
public class VehicleController {

    private final VehicleService vehicleService;
    private final VehicleStatusService vehicleStatusService;
    private final VehicleTelemetryService vehicleTelemetryService;

    public VehicleController(VehicleService vehicleService, VehicleStatusService vehicleStatusService,
            VehicleTelemetryService vehicleTelemetryService) {
        this.vehicleService = vehicleService;
        this.vehicleStatusService = vehicleStatusService;
        this.vehicleTelemetryService = vehicleTelemetryService;
    }

    // ==========================================
    // CRUD
    // ==========================================

    @Operation(summary = "Listar todos los vehículos", description = "Devuelve la lista completa de vehículos registrados en el sistema.")
    @ApiResponse(responseCode = "200", description = "Lista de vehículos")
    @GetMapping
    public List<VehicleDto> getAll() {
        return vehicleService.findAll();
    }

    @Operation(summary = "Obtener un vehículo por ID")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Vehículo encontrado"),
            @ApiResponse(responseCode = "404", description = "Vehículo no encontrado", content = @Content(schema = @Schema(hidden = true))) })
    @GetMapping("/{id}")
    public VehicleDto getOne(@Parameter(description = "ID del vehículo", example = "1") @PathVariable Long id) {
        return vehicleService.findById(id);
    }

    @Operation(summary = "Crear un vehículo")
    @ApiResponses({ @ApiResponse(responseCode = "201", description = "Vehículo creado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(hidden = true))) })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleDto create(@Valid @RequestBody CreateVehicleRequest request) {
        return vehicleService.create(request);
    }

    @Operation(summary = "Actualizar un vehículo")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Vehículo actualizado"),
            @ApiResponse(responseCode = "404", description = "Vehículo no encontrado", content = @Content(schema = @Schema(hidden = true))) })
    @PutMapping("/{id}")
    public VehicleDto update(@Parameter(description = "ID del vehículo", example = "1") @PathVariable Long id,
            @Valid @RequestBody UpdateVehicleRequest request) {
        return vehicleService.update(id, request);
    }

    @Operation(summary = "Eliminar un vehículo")
    @ApiResponse(responseCode = "204", description = "Vehículo eliminado")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "ID del vehículo", example = "1") @PathVariable Long id) {
        vehicleService.delete(id);
    }

    // ==========================================
    // ESTADO ACTUAL
    // ==========================================

    @Operation(summary = "Obtener el estado actual del vehículo", description = """
            Devuelve la última posición conocida, progreso, velocidad y estado del vehículo.
            Si el vehículo no tiene estado registrado, devuelve un DTO vacío con status IDLE.
            """)
    @ApiResponse(responseCode = "200", description = "Estado actual (o IDLE si no hay datos)")
    @GetMapping("/{id}/status")
    public ResponseEntity<VehicleStatusDto> getStatus(
            @Parameter(description = "ID del vehículo", example = "1") @PathVariable Long id) {
        return vehicleStatusService.getStatus(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(VehicleStatusDto.empty(id)));
    }

    // ==========================================
    // TELEMETRÍA
    // ==========================================

    @Operation(summary = "Obtener el histórico completo de telemetría", description = """
            Devuelve todos los puntos de telemetría registrados para el vehículo,
            ordenados cronológicamente. Opcionalmente se puede filtrar por rango de fechas
            usando los query params `from` y `to` (formato ISO-8601).
            """)
    @ApiResponse(responseCode = "200", description = "Lista de puntos de telemetría")
    @GetMapping("/{id}/telemetry")
    public ResponseEntity<List<TelemetryPointDto>> getTelemetry(
            @Parameter(description = "ID del vehículo", example = "1") @PathVariable Long id,
            @Parameter(description = "Fecha de inicio (ISO-8601)", example = "2026-09-29T00:00:00Z") @RequestParam(required = false) String from,
            @Parameter(description = "Fecha de fin (ISO-8601)", example = "2026-09-29T23:59:59Z") @RequestParam(required = false) String to) {

        if (from != null || to != null) {
            try {
                OffsetDateTime fromDt = from == null ? null : OffsetDateTime.parse(from);
                OffsetDateTime toDt = to == null ? null : OffsetDateTime.parse(to);
                if (fromDt != null && toDt != null && fromDt.isAfter(toDt)) {
                    throw new IllegalArgumentException("'from' must be before or equal to 'to'");
                }
                return ResponseEntity.ok(vehicleTelemetryService.getRange(id, fromDt, toDt));
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("'from' and 'to' must be ISO-8601 date-times", ex);
            }
        }
        return ResponseEntity.ok(vehicleTelemetryService.getAll(id));
    }

    @Operation(summary = "Obtener los últimos N puntos de telemetría", description = "Devuelve los N puntos más recientes del vehículo, en orden cronológico ascendente.")
    @ApiResponse(responseCode = "200", description = "Lista de puntos recientes")
    @GetMapping("/{id}/telemetry/latest")
    public ResponseEntity<List<TelemetryPointDto>> getLatestTelemetry(
            @Parameter(description = "ID del vehículo", example = "1") @PathVariable Long id,
            @Parameter(description = "Número máximo de puntos (1-500)", example = "50") @RequestParam(defaultValue = "50") int limit) {
        if (limit < 1 || limit > 500) {
            throw new IllegalArgumentException("'limit' must be between 1 and 500");
        }
        return ResponseEntity.ok(vehicleTelemetryService.getLatest(id, limit));
    }

    @Operation(summary = "Borrar el histórico de telemetría", description = "Elimina todos los puntos de telemetría del vehículo. Útil para pruebas.")
    @ApiResponse(responseCode = "200", description = "Histórico borrado")
    @DeleteMapping("/{id}/telemetry")
    public ResponseEntity<Map<String, Object>> clearTelemetry(
            @Parameter(description = "ID del vehículo", example = "1") @PathVariable Long id) {
        long deleted = vehicleTelemetryService.deleteAll(id);
        return ResponseEntity.ok(Map.of("vehicleId", id, "deleted", deleted));
    }
}