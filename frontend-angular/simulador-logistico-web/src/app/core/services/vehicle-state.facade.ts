import { Injectable, signal, computed } from '@angular/core';
import { websocketMessage } from '../models/websockeMessage.model';

@Injectable({ providedIn: 'root' })
export class VehicleStateService {

    private vehiclesMap = signal<Map<number, websocketMessage>>(new Map());

    readonly vehicles = computed(() => {
        const map = this.vehiclesMap();
        return Array.from(map.values()).sort((a, b) => a.vehicleId - b.vehicleId);
    });

    readonly activeCount = computed(() => this.vehiclesMap().size);

    getVehicle(vehicleId: number): websocketMessage | undefined {
        return this.vehiclesMap().get(vehicleId);
    }

    update(telemetry: websocketMessage): void {
        const currentMap = this.vehiclesMap();
        const newMap = new Map(currentMap);
        newMap.set(telemetry.vehicleId, telemetry);
        this.vehiclesMap.set(newMap);
    }

    clear(): void {
        this.vehiclesMap.set(new Map());
    }
}