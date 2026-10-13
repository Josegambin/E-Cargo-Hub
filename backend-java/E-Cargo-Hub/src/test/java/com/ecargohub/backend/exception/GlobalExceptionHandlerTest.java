package com.ecargohub.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    @Test
    void preservesStatusFromResponseStatusException() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/device-enrollments/claim");

        var response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Pairing code is invalid or already used"),
                request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().get("status"));
        assertEquals("UNAUTHORIZED", response.getBody().get("error"));
        assertEquals("Pairing code is invalid or already used", response.getBody().get("message"));
    }
}
