package com.ecargohub.backend.controller;

import com.ecargohub.backend.dto.device.VehicleDeviceClaimDto;
import com.ecargohub.backend.dto.device.VehicleDeviceClaimRequest;
import com.ecargohub.backend.dto.device.VehicleDeviceEnrollmentDto;
import com.ecargohub.backend.dto.device.VehicleDeviceSummaryDto;
import com.ecargohub.backend.dto.device.VehicleDisplayStateDto;
import com.ecargohub.backend.service.VehicleDeviceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class VehicleDeviceController {

    private final VehicleDeviceService deviceService;

    public VehicleDeviceController(VehicleDeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping("/vehicles/{vehicleId}/device-enrollments")
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleDeviceEnrollmentDto createEnrollment(@PathVariable Long vehicleId) {
        return deviceService.createEnrollment(vehicleId);
    }

    @GetMapping("/vehicles/{vehicleId}/devices")
    public List<VehicleDeviceSummaryDto> listDevices(@PathVariable Long vehicleId) {
        return deviceService.listForVehicle(vehicleId);
    }

    @DeleteMapping("/devices/{deviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeDevice(@PathVariable UUID deviceId) {
        deviceService.revoke(deviceId);
    }

    @PostMapping("/device-enrollments/claim")
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleDeviceClaimDto claim(@Valid @RequestBody VehicleDeviceClaimRequest request) {
        return deviceService.claim(request);
    }

    @GetMapping("/device/state")
    public VehicleDisplayStateDto getState(
            @RequestHeader(name = "X-Vehicle-Device-Token", required = false) String deviceToken) {
        return deviceService.state(deviceToken);
    }
}
