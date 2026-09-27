package com.ecargohub.backend.dto.route;

import java.util.List;

public record RouteResponseDto(
    List<double[]> coordinates, // Lista de puntos [latitud, longitud] que forman el camino
    double distanceInMeters,    // Distancia total de la ruta
    double timeInSeconds        // Tiempo estimado de viaje
) {}
