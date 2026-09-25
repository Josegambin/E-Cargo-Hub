package com.ecargohub.backend.repository;

import com.ecargohub.backend.entity.VehicleCommandEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface VehicleCommandRepository extends JpaRepository<VehicleCommandEntity, Long> {}
