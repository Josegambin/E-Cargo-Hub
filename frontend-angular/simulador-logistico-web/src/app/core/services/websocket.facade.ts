import { Injectable, signal } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Subject } from 'rxjs';
import { VehicleTelemetryMessage, FleetEvent } from '../models/VehicleTelemetryMessage.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class WebSocketService {

    readonly connected = signal<boolean>(false);

    private telemetrySubject = new Subject<VehicleTelemetryMessage>();
    private fleetEventSubject = new Subject<FleetEvent>();

    private client: Client | null = null;
    private subscriptions = new Map<string, StompSubscription>();

    /**
     * Cola de suscripciones pendientes.
     * Si alguien se suscribe antes de que STOMP esté conectado,
     * se guarda aquí y se aplica automáticamente al conectar.
     */
    private pendingSubscriptions = new Set<string>();

    private fleetEventsSubscribed = false;

    get telemetry$() { return this.telemetrySubject.asObservable(); }
    get fleetEvents$() { return this.fleetEventSubject.asObservable(); }

    // ==========================================
    // CONEXIÓN
    // ==========================================

    connect(): void {
        if (this.client?.active) {
            console.warn('⚠️ WebSocket ya está conectado');
            return;
        }

        console.log('🔌 Conectando WebSocket a', environment.wsUrl);

        this.client = new Client({
            webSocketFactory: () => new SockJS(environment.wsUrl),
            reconnectDelay: 5000,
            heartbeatIncoming: 10000,
            heartbeatOutgoing: 10000,

            onConnect: () => {
                console.log('🟢 WebSocket conectado');
                this.connected.set(true);

                // 🔄 Aplicar suscripciones pendientes
                this.applyPendingSubscriptions();
            },

            onDisconnect: () => {
                console.log('🔴 WebSocket desconectado');
                this.connected.set(false);
            },

            onStompError: (frame) => {
                console.error('❌ Error STOMP:', frame.headers['message'], frame.body);
            },

            onWebSocketError: (event) => {
                console.error('❌ Error WebSocket:', event);
            }
        });

        this.client.activate();
    }

    disconnect(): void {
        this.subscriptions.forEach(sub => sub.unsubscribe());
        this.subscriptions.clear();
        this.pendingSubscriptions.clear();
        this.client?.deactivate();
        this.client = null;
        this.connected.set(false);
        console.log('🔌 WebSocket desconectado');
    }

    // ==========================================
    // SUSCRIPCIONES
    // ==========================================

    /**
     * Aplica las suscripciones que estaban pendientes
     * (porque el cliente STOMP aún no había conectado).
     */
    private applyPendingSubscriptions(): void {
        if (!this.client || !this.client.connected) {
            return;
        }

        // Suscripciones de vehículos pendientes
        this.pendingSubscriptions.forEach(topic => {
            this.doSubscribe(topic);
        });
        this.pendingSubscriptions.clear();

        // Suscripción a eventos de flota
        if (this.fleetEventsSubscribed && !this.subscriptions.has('/topic/fleet-status')) {
            this.doSubscribeFleet();
        }
    }

    subscribeToVehicle(vehicleId: number): void {
        const topic = `/topic/vehicle-status/${vehicleId}`;

        if (this.subscriptions.has(topic)) {
            return;   // ya suscrito
        }

        // Si el cliente está conectado, suscribir ya
        // Si no, dejar pendiente
        if (this.client?.connected) {
            this.doSubscribe(topic);
        } else {
            this.pendingSubscriptions.add(topic);
        }
    }

    private doSubscribe(topic: string): void {
        if (!this.client) return;

        const sub = this.client.subscribe(topic, (msg: IMessage) => {
            try {
                const telemetry: VehicleTelemetryMessage = JSON.parse(msg.body);
                this.telemetrySubject.next(telemetry);
            } catch (e) {
                console.error('❌ Error parseando telemetría:', e);
            }
        });

        this.subscriptions.set(topic, sub);
        console.log(`📡 Suscrito a ${topic}`);
    }

    unsubscribeFromVehicle(vehicleId: number): void {
        const topic = `/topic/vehicle-status/${vehicleId}`;
        const sub = this.subscriptions.get(topic);
        if (sub) {
            sub.unsubscribe();
            this.subscriptions.delete(topic);
            console.log(`🔕 Desuscrito de ${topic}`);
        }
    }

    subscribeToFleetEvents(): void {
        this.fleetEventsSubscribed = true;

        if (this.subscriptions.has('/topic/fleet-status')) {
            return;   // ya suscrito
        }

        if (this.client?.connected) {
            this.doSubscribeFleet();
        } else {
            console.log('⏳ Suscripción a fleet-status pendiente de conexión');
        }
    }

    private doSubscribeFleet(): void {
        if (!this.client) return;

        const sub = this.client.subscribe('/topic/fleet-status', (msg: IMessage) => {
            try {
                const event: FleetEvent = JSON.parse(msg.body);
                this.fleetEventSubject.next(event);
            } catch (e) {
                console.error('❌ Error parseando evento de flota:', e);
            }
        });

        this.subscriptions.set('/topic/fleet-status', sub);
        console.log('📢 Suscrito a /topic/fleet-status');
    }
}