package com.ecargohub.backend.interfaces;

import com.ecargohub.backend.dto.route.CreateRouteRequest;
import com.ecargohub.backend.dto.route.RouteDto;

import java.util.List;

public interface RouteService {

    List<RouteDto> findAll();

    RouteDto findById(Long routeId);

    RouteDto create(CreateRouteRequest request);

    RouteDto calculateRoute(Long routeId);

    void delete(Long routeId);
}