/**
 * Modelos para los mensajes que viajan por WebSocket.
 *
 * Los DTOs de la API REST vienen del cliente generado en `src/app/api/model/`.
 * Estos modelos son SOLO para los mensajes STOMP que el backend emite por WebSocket.
 */

export interface VehicleTelemetryMessage {
    vehicleId: number;
    latitude: number | null;
    longitude: number | null;
    progress: number | null;
    speedKmh: number;
    status: string;
    timestamp: number;
}

export type VehicleStatus = 'IDLE' | 'EN_RUTA' | 'PAUSADO' | 'COMPLETADO' | 'STOPPED';

export type FleetEventType =
    | 'VEHICLE_STARTED'
    | 'VEHICLE_PAUSED'
    | 'VEHICLE_RESUMED'
    | 'VEHICLE_STOPPED'
    | 'VEHICLE_COMPLETED';

export interface FleetEvent {
    eventType: FleetEventType;
    vehicleId: number;
    status: string;
    message: string;
    timestamp: number;
}