import { Injectable, signal } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { Subject } from 'rxjs';
import { websocketMessage, FleetEvent } from '../models/websockeMessage.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class WebSocketService {

    // 🔌 Estado de conexión
    readonly connected = signal<boolean>(false);

    // 📢 Últimos 50 eventos de flota (para el dashboard)
    readonly lastEvents = signal<FleetEvent[]>([]);

    // 📡 Streams para telemetría y eventos
    private telemetrySubject = new Subject<websocketMessage>();
    private fleetEventSubject = new Subject<FleetEvent>();

    private client: Client | null = null;
    private subscriptions = new Map<string, StompSubscription>();
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

        console.log('🔌 Conectando WebSocket nativo a', environment.wsUrlNative);

        this.client = new Client({
            brokerURL: environment.wsUrlNative,   // 👈 WebSocket nativo
            reconnectDelay: 5000,
            heartbeatIncoming: 10000,
            heartbeatOutgoing: 10000,

            onConnect: () => {
                console.log('🟢 WebSocket conectado');
                this.connected.set(true);
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

    private applyPendingSubscriptions(): void {
        if (!this.client || !this.client.connected) return;

        this.pendingSubscriptions.forEach(topic => this.doSubscribe(topic));
        this.pendingSubscriptions.clear();

        if (this.fleetEventsSubscribed && !this.subscriptions.has('/topic/fleet-status')) {
            this.doSubscribeFleet();
        }
    }

    subscribeToVehicle(vehicleId: number): void {
        const topic = `/topic/vehicle-status/${vehicleId}`;

        if (this.subscriptions.has(topic)) return;

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
                const telemetry: websocketMessage = JSON.parse(msg.body);
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

        if (this.subscriptions.has('/topic/fleet-status')) return;

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

                // Emitir al stream
                this.fleetEventSubject.next(event);

                // Guardar últimos 50 eventos (para el dashboard)
                const current = this.lastEvents();
                this.lastEvents.set([event, ...current].slice(0, 50));

            } catch (e) {
                console.error('❌ Error parseando evento de flota:', e);
            }
        });

        this.subscriptions.set('/topic/fleet-status', sub);
        console.log('📢 Suscrito a /topic/fleet-status');
    }
}

export type { FleetEvent };
export type { websocketMessage };
