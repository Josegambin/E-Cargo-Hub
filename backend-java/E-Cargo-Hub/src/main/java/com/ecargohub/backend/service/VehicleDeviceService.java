package com.ecargohub.backend.service;

import com.ecargohub.backend.dto.device.VehicleDeviceClaimDto;
import com.ecargohub.backend.dto.device.VehicleDeviceClaimRequest;
import com.ecargohub.backend.dto.device.VehicleDeviceEnrollmentDto;
import com.ecargohub.backend.dto.device.VehicleDisplayStateDto;
import com.ecargohub.backend.dto.status.VehicleStatusDto;
import com.ecargohub.backend.domain.enums.VehicleEnergyTypeEnum;
import com.ecargohub.backend.entity.VehicleDeviceEnrollmentEntity;
import com.ecargohub.backend.entity.VehicleDeviceEntity;
import com.ecargohub.backend.exception.ResourceNotFoundException;
import com.ecargohub.backend.repository.VehicleDeviceEnrollmentRepository;
import com.ecargohub.backend.repository.VehicleDeviceRepository;
import com.ecargohub.backend.repository.VehicleRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class VehicleDeviceService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    private final VehicleDeviceEnrollmentRepository enrollmentRepository;
    private final VehicleDeviceRepository deviceRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleStatusService statusService;
    private final SimulationRegistry simulationRegistry;
    private final String mobileAppUrl;

    public VehicleDeviceService(VehicleDeviceEnrollmentRepository enrollmentRepository,
            VehicleDeviceRepository deviceRepository, VehicleRepository vehicleRepository,
            VehicleStatusService statusService, SimulationRegistry simulationRegistry,
            @Value("${app.mobile-app-url:http://localhost:4200/vehicle-display}") String mobileAppUrl) {
        this.enrollmentRepository = enrollmentRepository;
        this.deviceRepository = deviceRepository;
        this.vehicleRepository = vehicleRepository;
        this.statusService = statusService;
        this.simulationRegistry = simulationRegistry;
        this.mobileAppUrl = mobileAppUrl;
    }

    @Transactional
    public VehicleDeviceEnrollmentDto createEnrollment(Long vehicleId) {
        var vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + vehicleId));
        String code = randomToken(32);
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(10);
        String separator = mobileAppUrl.contains("?") ? "&" : "?";
        String pairingUrl = mobileAppUrl + separator + "pairingCode=" + code;
        String qrSvg = createQrSvg(pairingUrl);

        VehicleDeviceEnrollmentEntity enrollment = new VehicleDeviceEnrollmentEntity();
        enrollment.setVehicle(vehicle);
        enrollment.setCodeHash(hash(code));
        enrollment.setCreatedAt(OffsetDateTime.now());
        enrollment.setExpiresAt(expiresAt);
        enrollmentRepository.save(enrollment);

        return new VehicleDeviceEnrollmentDto(vehicleId, code, pairingUrl, qrSvg, expiresAt);
    }

    @Transactional
    public VehicleDeviceClaimDto claim(VehicleDeviceClaimRequest request) {
        VehicleDeviceEnrollmentEntity enrollment = enrollmentRepository.lockByCodeHash(hash(request.pairingCode()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Pairing code is invalid or already used"));
        OffsetDateTime now = OffsetDateTime.now();
        if (enrollment.getClaimedAt() != null || !enrollment.getExpiresAt().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Pairing code is expired or already used");
        }

        String token = randomToken(32);
        UUID deviceId = UUID.randomUUID();
        VehicleDeviceEntity device = new VehicleDeviceEntity();
        device.setId(deviceId);
        device.setVehicle(enrollment.getVehicle());
        device.setCredentialHash(hash(token));
        device.setName(request.deviceName().trim());
        device.setEnabled(true);
        device.setCreatedAt(now);
        deviceRepository.save(device);

        enrollment.setClaimedAt(now);
        enrollmentRepository.save(enrollment);
        return new VehicleDeviceClaimDto(deviceId, enrollment.getVehicle().getId(), token);
    }

    @Transactional(readOnly = true)
    public VehicleDisplayStateDto state(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Device token is required");
        }
        VehicleDeviceEntity device = deviceRepository.findByCredentialHashAndEnabledTrue(hash(token))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid device token"));
        Long vehicleId = device.getVehicle().getId();
        VehicleStatusDto storedStatus = statusService.getStatus(vehicleId)
                .orElseGet(() -> VehicleStatusDto.empty(vehicleId));
        VehicleEnergyTypeEnum energyType = device.getVehicle().getEnergyType();
        VehicleStatusDto status = new VehicleStatusDto(storedStatus.vehicleId(), storedStatus.latitude(),
                storedStatus.longitude(), storedStatus.progress(), storedStatus.speedKmh(),
                storedStatus.energyPercent() == null ? device.getVehicle().getEnergyPercent()
                        : storedStatus.energyPercent(),
                storedStatus.energyType() == null ? energyType : storedStatus.energyType(),
                storedStatus.gearPosition() == null ? "P" : storedStatus.gearPosition(), storedStatus.status(),
                storedStatus.updatedAt());
        Double energyCapacity = device.getVehicle().getEnergyCapacity();
        Double consumptionPer100 = device.getVehicle().getEnergyConsumptionPer100();
        Double energyPercent = status.energyPercent();
        Double estimatedRangeKm = energyCapacity == null || energyCapacity <= 0 || consumptionPer100 == null
                || consumptionPer100 <= 0 || energyPercent == null ? null
                        : energyCapacity * energyPercent / consumptionPer100;
        int estimatedRpm = estimateRpm(status.speedKmh(), status.gearPosition());
        return new VehicleDisplayStateDto(status, simulationRegistry.getControls(vehicleId),
                simulationRegistry.getRouteCoordinates(vehicleId), estimatedRangeKm, estimatedRpm);
    }

    private int estimateRpm(Double speedKmh, String gearPosition) {
        if (speedKmh == null || speedKmh <= 0) {
            return 0;
        }
        int gear = 1;
        if (gearPosition != null && gearPosition.matches("D[1-6]")) {
            gear = Character.digit(gearPosition.charAt(1), 10);
        }
        double[] ratios = { 3.8, 2.3, 1.5, 1.1, 0.85, 0.65 };
        double wheelRpm = speedKmh * 1000.0 / 60.0 / 2.0;
        return (int) Math.round(wheelRpm * ratios[gear - 1] * 3.5);
    }

    private String randomToken(int bytes) {
        byte[] value = new byte[bytes];
        RANDOM.nextBytes(value);
        return HEX.formatHex(value);
    }

    private String hash(String token) {
        try {
            return HEX.formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private String createQrSvg(String value) {
        try {
            var matrix = new QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, 1, 1,
                    Map.of(EncodeHintType.MARGIN, 4));
            StringBuilder path = new StringBuilder();
            for (int y = 0; y < matrix.getHeight(); y++) {
                for (int x = 0; x < matrix.getWidth();) {
                    if (!matrix.get(x, y)) {
                        x++;
                        continue;
                    }
                    int start = x;
                    while (x < matrix.getWidth() && matrix.get(x, y)) {
                        x++;
                    }
                    int width = x - start;
                    path.append('M').append(start).append(' ').append(y).append('h').append(width).append("v1h-")
                            .append(width).append('z');
                }
            }
            return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + matrix.getWidth() + " "
                    + matrix.getHeight() + "\" shape-rendering=\"crispEdges\"><rect width=\"100%\" height=\"100%\" "
                    + "fill=\"#fff\"/><path d=\"" + path + "\"/></svg>";
        } catch (WriterException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate pairing QR",
                    exception);
        }
    }
}
