import { Injectable, signal, computed } from '@angular/core';
import { VehicleTelemetryMessage } from '../models/VehicleTelemetryMessage.model';

@Injectable({ providedIn: 'root' })
export class VehicleStateService {

    private vehiclesMap = signal<Map<number, VehicleTelemetryMessage>>(new Map());

    readonly vehicles = computed(() => {
        const map = this.vehiclesMap();
        return Array.from(map.values()).sort((a, b) => a.vehicleId - b.vehicleId);
    });

    readonly activeCount = computed(() => this.vehiclesMap().size);

    getVehicle(vehicleId: number): VehicleTelemetryMessage | undefined {
        return this.vehiclesMap().get(vehicleId);
    }

    update(telemetry: VehicleTelemetryMessage): void {
        const currentMap = this.vehiclesMap();
        const newMap = new Map(currentMap);
        newMap.set(telemetry.vehicleId, telemetry);
        this.vehiclesMap.set(newMap);
    }

    clear(): void {
        this.vehiclesMap.set(new Map());
    }
}