package com.ecargohub.backend.interfaces;

import com.ecargohub.backend.dto.warehouse.CreateWarehouseRequest;
import com.ecargohub.backend.dto.warehouse.UpdateWarehouseRequest;
import com.ecargohub.backend.dto.warehouse.WarehouseDto;

import java.util.List;

public interface WarehouseService {

    List<WarehouseDto> findAll();

    WarehouseDto findById(Long warehouseId);

    WarehouseDto create(CreateWarehouseRequest request);

    WarehouseDto update(Long warehouseId, UpdateWarehouseRequest request);

    void delete(Long warehouseId);
}