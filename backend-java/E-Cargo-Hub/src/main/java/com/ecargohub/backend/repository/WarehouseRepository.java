package com.ecargohub.backend.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecargohub.backend.entity.WarehouseEntity;


@Repository
public interface WarehouseRepository  extends JpaRepository<WarehouseEntity, Long> {   

}