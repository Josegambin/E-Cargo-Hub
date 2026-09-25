package com.ecargohub.backend.interfaces;

import com.ecargohub.backend.domain.enums.AlertSeverityEnum;
import com.ecargohub.backend.dto.alert.VehicleAlertDto;

import java.util.List;

public interface AlertService {

    List<VehicleAlertDto> findAlerts(Long vehicleId, Long simulationId, AlertSeverityEnum severity);

    VehicleAlertDto create(VehicleAlertDto alert);
}