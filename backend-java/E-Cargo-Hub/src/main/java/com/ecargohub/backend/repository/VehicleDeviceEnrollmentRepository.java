package com.ecargohub.backend.repository;

import com.ecargohub.backend.entity.VehicleDeviceEnrollmentEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface VehicleDeviceEnrollmentRepository extends JpaRepository<VehicleDeviceEnrollmentEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select enrollment from VehicleDeviceEnrollmentEntity enrollment where enrollment.codeHash = :codeHash")
    Optional<VehicleDeviceEnrollmentEntity> lockByCodeHash(@Param("codeHash") String codeHash);
}
