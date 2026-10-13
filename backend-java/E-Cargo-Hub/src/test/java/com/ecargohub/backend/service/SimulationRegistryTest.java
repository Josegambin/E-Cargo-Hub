package com.ecargohub.backend.service;

import com.ecargohub.backend.domain.enums.VehicleCommandTypeEnum;
import com.ecargohub.backend.dto.vehicle.VehicleControlStateDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class SimulationRegistryTest {

    private final SimulationRegistry registry = new SimulationRegistry();

    @Test
    void controlsDefaultToIgnitionOnAndAccessoriesOff() {
        VehicleControlStateDto state = registry.getControls(7L);

        assertTrue(state.ignitionOn());
        assertFalse(state.headlightsOn());
        assertFalse(state.highBeamsOn());
        assertFalse(state.hazardLightsOn());
        assertEquals("OFF", state.turnSignal());
        assertFalse(state.wipersOn());
        assertFalse(state.climateOn());
        assertEquals(22.0, state.climateTemperatureC());
        assertEquals(0, state.climateFanSpeed());
        assertEquals("AUTO", state.climateMode());
    }

    @Test
    void controlUpdatesPreserveUnrelatedValues() {
        registry.updateControl(7L, VehicleCommandTypeEnum.SET_HEADLIGHTS, 1.0);
        VehicleControlStateDto state = registry.updateControl(7L, VehicleCommandTypeEnum.SET_TURN_SIGNAL, 2.0);

        assertTrue(state.headlightsOn());
        assertEquals("RIGHT", state.turnSignal());
        assertTrue(state.ignitionOn());
        assertFalse(state.wipersOn());
    }

    @Test
    void climateControlUpdatesPreserveOtherControls() {
        registry.updateControl(7L, VehicleCommandTypeEnum.SET_CLIMATE_POWER, 1.0);
        registry.updateControl(7L, VehicleCommandTypeEnum.SET_CLIMATE_TEMPERATURE, 19.5);
        registry.updateControl(7L, VehicleCommandTypeEnum.SET_CLIMATE_FAN_SPEED, 3.0);
        VehicleControlStateDto state = registry.updateControl(7L, VehicleCommandTypeEnum.SET_CLIMATE_MODE, 2.0);

        assertTrue(state.climateOn());
        assertEquals(19.5, state.climateTemperatureC());
        assertEquals(3, state.climateFanSpeed());
        assertEquals("HEAT", state.climateMode());
        assertTrue(state.ignitionOn());
    }

    @Test
    void routeCoordinatesAreCopiedWhenStoredAndReturned() {
        double[] coordinate = { 38.1, -0.4 };
        registry.setRouteCoordinates(7L, java.util.List.of(coordinate));
        coordinate[0] = 0.0;

        var firstRead = registry.getRouteCoordinates(7L);
        assertEquals(38.1, firstRead.get(0)[0]);
        assertNotSame(coordinate, firstRead.get(0));
        firstRead.get(0)[0] = 0.0;
        assertEquals(38.1, registry.getRouteCoordinates(7L).get(0)[0]);
    }
}
