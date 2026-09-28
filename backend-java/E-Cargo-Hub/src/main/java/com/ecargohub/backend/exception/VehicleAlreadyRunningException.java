package com.ecargohub.backend.exception;

public class VehicleAlreadyRunningException extends RuntimeException {
    public VehicleAlreadyRunningException(Long vehicleId) {
        super("Vehicle " + vehicleId + " is already running. Wait for it to finish or send STOP first.");
    }
}