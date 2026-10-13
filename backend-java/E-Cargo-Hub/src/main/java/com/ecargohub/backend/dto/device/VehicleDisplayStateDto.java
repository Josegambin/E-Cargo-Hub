package com.ecargohub.backend.dto.device;

import com.ecargohub.backend.dto.status.VehicleStatusDto;
import com.ecargohub.backend.dto.vehicle.VehicleControlStateDto;
import java.util.List;

public record VehicleDisplayStateDto(VehicleStatusDto vehicle, VehicleControlStateDto controls,
        List<double[]> routeCoordinates, Double estimatedRangeKm, Integer estimatedRpm) {
}
