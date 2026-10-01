import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CommandsService as CommandApiService } from '../../api/api/commands.service';
import { VehicleCommand } from '../../api/model/vehicleCommand';
import { VehicleCommandRequest } from '../../api/model/vehicleCommandRequest';

/**
 * Fachada sobre el cliente generado de Commands.
 */
@Injectable({ providedIn: 'root' })
export class CommandFacade {

    private api = inject(CommandApiService);

    /**
     * Envía un comando a un vehículo.
     */
    send(vehicleId: number, request: VehicleCommandRequest): Observable<VehicleCommand> {
        return this.api.sendVehicleCommand(vehicleId, request);
    }

    /**
     * Obtiene el historial de comandos de un vehículo.
     */
    getHistory(vehicleId: number): Observable<VehicleCommand[]> {
        return this.api.getVehicleCommandHistory(vehicleId);
    }
}