package com.ecargohub.backend.repository;

import com.ecargohub.backend.entity.VehicleDeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VehicleDeviceRepository extends JpaRepository<VehicleDeviceEntity, UUID> {

    Optional<VehicleDeviceEntity> findByCredentialHashAndEnabledTrue(String credentialHash);
}
