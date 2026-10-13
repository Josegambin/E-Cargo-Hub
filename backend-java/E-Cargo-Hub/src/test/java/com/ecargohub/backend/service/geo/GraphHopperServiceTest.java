package com.ecargohub.backend.service.geo;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphHopperServiceTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void parsesSpeedLimitsAcrossEveryRouteSegment() throws Exception {
        var details = jsonMapper.readTree("[[0,1,50],[1,3,80]]");

        assertEquals(List.of(50.0, 80.0, 80.0), GraphHopperService.extractSpeedLimits(details, 3));
    }

    @Test
    void parsesNumericSpeedLimitsReturnedAsStrings() throws Exception {
        var details = jsonMapper.readTree("[[0,1,\"50\"],[1,3,\"80.5\"]]");

        assertEquals(List.of(50.0, 80.5, 80.5), GraphHopperService.extractSpeedLimits(details, 3));
    }

    @Test
    void usesVehicleMaximumOnlyForExplicitNullSpeedLimits() throws Exception {
        var details = jsonMapper.readTree("[[0,1,null],[1,3,80]]");

        assertEquals(List.of(100.0, 80.0, 80.0),
                GraphHopperService.extractSpeedLimits(details, 3, 100.0));
    }

    @Test
    void rejectsMissingOrNonNumericSpeedLimits() throws Exception {
        var incomplete = jsonMapper.readTree("[[0,1,50]]");
        var nonNumeric = jsonMapper.readTree("[[0,2,\"unlimited\"]]");
        var nullValue = jsonMapper.readTree("[[0,2,null]]");

        assertThrows(IllegalStateException.class, () -> GraphHopperService.extractSpeedLimits(incomplete, 2));
        assertThrows(IllegalStateException.class,
                () -> GraphHopperService.extractSpeedLimits(nullValue, 2));
        var exception = assertThrows(IllegalStateException.class,
                () -> GraphHopperService.extractSpeedLimits(nonNumeric, 2));
        assertTrue(exception.getMessage().contains("value=\"unlimited\""));
    }

    @Test
    void rejectsAnInvalidVehicleMaximumSpeedFallback() throws Exception {
        var details = jsonMapper.readTree("[[0,1,null]]");

        assertThrows(IllegalStateException.class,
                () -> GraphHopperService.extractSpeedLimits(details, 1, 0.0));
    }
}
