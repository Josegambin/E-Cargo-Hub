package com.ecargohub.backend.dto.vehicle;

public record VehicleControlStateDto(Long vehicleId, boolean ignitionOn, boolean headlightsOn, boolean highBeamsOn,
        boolean hazardLightsOn, String turnSignal, boolean wipersOn, boolean climateOn, double climateTemperatureC,
        int climateFanSpeed, String climateMode) {
}
