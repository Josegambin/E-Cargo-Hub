package com.ecargohub.backend.mapper;

import com.ecargohub.backend.api.model.CreateVehicleRequest;
import com.ecargohub.backend.api.model.UpdateVehicleRequest;
import com.ecargohub.backend.api.model.Vehicle;
import com.ecargohub.backend.domain.VehicleStatus;
import com.ecargohub.backend.domain.VehicleType;
import com.ecargohub.backend.entity.VehicleEntity;

import org.springframework.stereotype.Component;

@Component
public class VehicleMapper {

    public VehicleEntity toEntity(CreateVehicleRequest request) {

        VehicleEntity entity = new VehicleEntity();

        entity.setName(request.getName());
        entity.setType(toDomainType(request.getType()));
        entity.setMaxSpeed(request.getMaxSpeed());

        // Un vehículo recién creado empieza detenido.
        entity.setStatus(VehicleStatus.IDLE);

        return entity;
    }

    public void updateEntity(
            VehicleEntity entity,
            UpdateVehicleRequest request) {

        entity.setName(request.getName());
        entity.setType(toDomainType(request.getType()));
        entity.setMaxSpeed(request.getMaxSpeed());
    }

    public Vehicle toResponse(VehicleEntity entity) {

        Vehicle response = new Vehicle();

        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setType(
                com.ecargohub.backend.api.model.VehicleType
                        .valueOf(entity.getType().name()));
        response.setMaxSpeed(entity.getMaxSpeed());
        response.setStatus(
                com.ecargohub.backend.api.model.VehicleStatus
                        .valueOf(entity.getStatus().name()));

        /*
         * currentPosition no se persiste en VehicleEntity.
         *
         * La posición actual pertenecerá posteriormente al
         * subsistema de posiciones/simulación.
         */
        response.setCurrentPosition(null);

        return response;
    }

    private VehicleType toDomainType(
            com.ecargohub.backend.api.model.VehicleType type) {

        return VehicleType.valueOf(type.name());
    }
}