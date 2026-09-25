package com.ecargohub.backend.service;

import com.ecargohub.backend.dto.warehouse.CreateWarehouseRequest;
import com.ecargohub.backend.dto.warehouse.UpdateWarehouseRequest;
import com.ecargohub.backend.dto.warehouse.WarehouseDto;
import com.ecargohub.backend.entity.WarehouseEntity;
import com.ecargohub.backend.exception.ResourceNotFoundException;
import com.ecargohub.backend.mapper.WarehouseMapper;
import com.ecargohub.backend.repository.WarehouseRepository;
import com.ecargohub.backend.interfaces.WarehouseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseMapper warehouseMapper;

    public WarehouseServiceImpl(WarehouseRepository warehouseRepository,
                                WarehouseMapper warehouseMapper) {
        this.warehouseRepository = warehouseRepository;
        this.warehouseMapper = warehouseMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseDto> findAll() {
        return warehouseRepository.findAll().stream()
                .map(warehouseMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseDto findById(Long warehouseId) {
        return warehouseRepository.findById(warehouseId)
                .map(warehouseMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Warehouse not found: " + warehouseId));
    }

    @Override
    public WarehouseDto create(CreateWarehouseRequest request) {
        WarehouseEntity entity = warehouseMapper.toEntity(request);
        return warehouseMapper.toDto(warehouseRepository.save(entity));
    }

    @Override
    public WarehouseDto update(Long warehouseId, UpdateWarehouseRequest request) {
        WarehouseEntity entity = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Warehouse not found: " + warehouseId));
        warehouseMapper.updateEntity(entity, request);
        return warehouseMapper.toDto(warehouseRepository.save(entity));
    }

    @Override
    public void delete(Long warehouseId) {
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new ResourceNotFoundException("Warehouse not found: " + warehouseId);
        }
        warehouseRepository.deleteById(warehouseId);
    }
}