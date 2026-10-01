import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { TelemetryService as TelemetryApiService } from '../../api/api/telemetry.service';
import { TelemetryPoint } from '../../api/model/telemetryPoint';
import { TelemetryClearResult } from '../../api/model/telemetryClearResult';

/**
 * Fachada sobre el cliente generado de Telemetry.
 */
@Injectable({ providedIn: 'root' })
export class TelemetryFacade {

    private api = inject(TelemetryApiService);

    getTelemetry(vehicleId: number): Observable<TelemetryPoint[]> {
        return this.api.getVehicleTelemetry(vehicleId);
    }

    getRange(vehicleId: number, from: string, to: string): Observable<TelemetryPoint[]> {
        return this.api.getVehicleTelemetry(vehicleId, from, to);
    }

    getLatest(vehicleId: number, limit: number = 50): Observable<TelemetryPoint[]> {
        return this.api.getLatestVehicleTelemetry(vehicleId, limit);
    }

    clearTelemetry(vehicleId: number): Observable<TelemetryClearResult> {
        return this.api.clearVehicleTelemetry(vehicleId);
    }
}