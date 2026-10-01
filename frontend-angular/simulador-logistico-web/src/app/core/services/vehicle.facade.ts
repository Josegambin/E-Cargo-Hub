import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { VehiclesService as VehicleApiService } from '../../api/api/vehicles.service';
import { Vehicle } from '../../api/model/vehicle';
import { VehicleStatusDto } from '../../api/model/vehicleStatusDto';
import { CreateVehicleRequest } from '../../api/model/createVehicleRequest';
import { UpdateVehicleRequest } from '../../api/model/updateVehicleRequest';

/**
 * Fachada sobre el cliente generado de Vehicles.
 */
@Injectable({ providedIn: 'root' })
export class VehicleFacade {

    private api = inject(VehicleApiService);

    getAll(): Observable<Vehicle[]> {
        return this.api.getVehicles();
    }

    getById(id: number): Observable<Vehicle> {
        return this.api.getVehicle(id);
    }

    getStatus(id: number): Observable<VehicleStatusDto> {
        return this.api.getVehicleStatus(id);
    }

    create(request: CreateVehicleRequest): Observable<Vehicle> {
        return this.api.createVehicle(request);
    }

    update(id: number, request: UpdateVehicleRequest): Observable<Vehicle> {
        return this.api.updateVehicle(id, request);
    }

    delete(id: number): Observable<any> {
        return this.api.deleteVehicle(id);
    }
}