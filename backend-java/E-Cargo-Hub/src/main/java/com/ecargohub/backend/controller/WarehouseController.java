package com.ecargohub.backend.controller;

import com.ecargohub.backend.dto.warehouse.CreateWarehouseRequest;
import com.ecargohub.backend.dto.warehouse.UpdateWarehouseRequest;
import com.ecargohub.backend.dto.warehouse.WarehouseDto;
import com.ecargohub.backend.interfaces.WarehouseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @GetMapping
    public List<WarehouseDto> getAll() {
        return warehouseService.findAll();
    }

    @GetMapping("/{warehouseId}")
    public WarehouseDto getOne(@PathVariable Long warehouseId) {
        return warehouseService.findById(warehouseId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WarehouseDto create(@Valid @RequestBody CreateWarehouseRequest request) {
        return warehouseService.create(request);
    }

    @PutMapping("/{warehouseId}")
    public WarehouseDto update(@PathVariable Long warehouseId,
                               @Valid @RequestBody UpdateWarehouseRequest request) {
        return warehouseService.update(warehouseId, request);
    }

    @DeleteMapping("/{warehouseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long warehouseId) {
        warehouseService.delete(warehouseId);
    }
}