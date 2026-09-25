package com.ecargohub.backend.service;

import com.ecargohub.backend.domain.enums.RouteStatusEnum;
import com.ecargohub.backend.client.GraphHopperClient;
import com.ecargohub.backend.dto.route.CreateRouteRequest;
import com.ecargohub.backend.dto.route.RouteDto;
import com.ecargohub.backend.entity.RouteEntity;
import com.ecargohub.backend.exception.ResourceNotFoundException;
import com.ecargohub.backend.mapper.RouteMapper;
import com.ecargohub.backend.mapper.GeoMapper;
import com.ecargohub.backend.repository.RouteRepository;
import com.ecargohub.backend.interfaces.RouteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class RouteServiceImpl implements RouteService {

    private final RouteRepository routeRepository;
    private final RouteMapper routeMapper;
    private final GraphHopperClient graphHopperClient;
    private final GeoMapper geoMapper;

    public RouteServiceImpl(RouteRepository routeRepository,
                            RouteMapper routeMapper) {
        this.routeRepository = routeRepository;
        this.routeMapper = routeMapper;
        this.graphHopperClient = new GraphHopperClient();
        this.geoMapper = null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RouteDto> findAll() {
        return routeRepository.findAll().stream()
                .map(routeMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RouteDto findById(Long routeId) {
        return routeRepository.findById(routeId)
                .map(routeMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Route not found: " + routeId));
    }

    @Override
    public RouteDto create(CreateRouteRequest request) {
        RouteEntity entity = routeMapper.toEntity(request);
        return routeMapper.toDto(routeRepository.save(entity));
    }

    @Override
    public void delete(Long routeId) {
        if (!routeRepository.existsById(routeId)) {
            throw new ResourceNotFoundException("Route not found: " + routeId);
        }
        routeRepository.deleteById(routeId);
    }

    @Override
public RouteDto calculateRoute(Long routeId) {
    RouteEntity entity = routeRepository.findById(routeId)
            .orElseThrow(() -> new ResourceNotFoundException("Route not found: " + routeId));

    entity.setStatus(RouteStatusEnum.CALCULATING);
    routeRepository.save(entity);

    try {
        GraphHopperClient.GraphHopperRouteResult result =
                graphHopperClient.calculateRoute(
                        routeMapper.toDto(entity).origin(),
                        routeMapper.toDto(entity).destination()
                );

        entity.setDistanceMeters(result.distanceMeters());
        entity.setDurationSeconds(result.durationSeconds());
        entity.setGeometry(geoMapper.toEntity(result.geometry()));
        entity.setStatus(RouteStatusEnum.READY);

    } catch (Exception e) {
        entity.setStatus(RouteStatusEnum.ERROR);
        throw new IllegalStateException("Error calculando ruta: " + e.getMessage(), e);
    }

    return routeMapper.toDto(routeRepository.save(entity));
}
}