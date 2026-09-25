package com.ecargohub.backend.controller;

import com.ecargohub.backend.dto.route.CreateRouteRequest;
import com.ecargohub.backend.dto.route.RouteDto;
import com.ecargohub.backend.interfaces.RouteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping
    public List<RouteDto> getAll() {
        return routeService.findAll();
    }

    @GetMapping("/{routeId}")
    public RouteDto getOne(@PathVariable Long routeId) {
        return routeService.findById(routeId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RouteDto create(@Valid @RequestBody CreateRouteRequest request) {
        return routeService.create(request);
    }

    @DeleteMapping("/{routeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long routeId) {
        routeService.delete(routeId);
    }
}