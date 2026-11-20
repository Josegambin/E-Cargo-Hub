package com.ecargohub.backend.client;

import com.ecargohub.backend.exception.ExternalServiceUnavailableException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class GraphHopperClientTest {

    @Test
    void rejectsRouteRequestsWhenTheApiKeyIsNotConfigured() {
        GraphHopperClient client = new GraphHopperClient("https://graphhopper.com/api/1", "");

        assertThrows(ExternalServiceUnavailableException.class, () -> client.calculateRoute(null, null));
    }
}
