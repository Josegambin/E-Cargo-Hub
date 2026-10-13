import { Component, DestroyRef, PLATFORM_ID, inject, signal } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { DecimalPipe, isPlatformBrowser } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, catchError, exhaustMap, of, switchMap, timer } from 'rxjs';
import { environment } from '../../../environments/environment';

interface VehicleDisplayState {
    vehicle: {
        vehicleId: number;
        status: string;
        speedKmh: number;
        progress: number;
        energyPercent: number;
        energyType: 'BATTERY_ELECTRIC' | 'FUEL';
        gearPosition: string;
    };
    controls: {
        ignitionOn: boolean;
        headlightsOn: boolean;
        highBeamsOn: boolean;
        hazardLightsOn: boolean;
        turnSignal: string;
        wipersOn: boolean;
        climateOn: boolean;
        climateTemperatureC: number;
        climateFanSpeed: number;
        climateMode: string;
    };
}

interface DeviceClaim {
    vehicleId: number;
    deviceToken: string;
}

@Component({
    selector: 'app-vehicle-display',
    standalone: true,
    imports: [DecimalPipe],
    template: `
        <main class="min-h-screen bg-slate-950 p-5 text-white sm:p-10">
            <section class="mx-auto max-w-3xl">
                <h1 class="text-2xl font-bold tracking-wide">E-CARGO HUB</h1>
                <p class="mb-6 text-sm text-slate-400">PANTALLA DEL VEHÍCULO · SOLO LECTURA</p>

                @if (state(); as current) {
                    <div class="rounded-3xl bg-slate-900 p-6 shadow-xl sm:p-10">
                        <div class="text-6xl font-bold tabular-nums sm:text-8xl">
                            {{ current.vehicle.speedKmh | number:'1.0-1' }}
                            <span class="text-2xl text-slate-400">km/h</span>
                        </div>
                        <div class="mt-3 text-xl text-cyan-300">
                            {{ current.vehicle.status }} · Marcha {{ current.vehicle.gearPosition }}
                        </div>
                        <div class="mt-2 text-slate-300">Trayecto {{ current.vehicle.progress }}%</div>
                        <div class="mt-6 grid grid-cols-2 gap-3 sm:grid-cols-3">
                            <div class="status-tile">
                                {{ current.vehicle.energyType === 'FUEL' ? 'Combustible' : 'Batería' }}
                                <strong>{{ current.vehicle.energyPercent | number:'1.0-1' }}%</strong>
                            </div>
                            <div class="status-tile">Encendido<strong>{{ onOff(current.controls.ignitionOn) }}</strong></div>
                            <div class="status-tile">Luces<strong>{{ onOff(current.controls.headlightsOn) }}</strong></div>
                            <div class="status-tile">Largas<strong>{{ onOff(current.controls.highBeamsOn) }}</strong></div>
                            <div class="status-tile">Intermitente<strong>{{ current.controls.turnSignal }}</strong></div>
                            <div class="status-tile">Limpiaparabrisas<strong>{{ onOff(current.controls.wipersOn) }}</strong></div>
                            <div class="status-tile">Climatizador<strong>{{ onOff(current.controls.climateOn) }}</strong></div>
                            <div class="status-tile">Temperatura<strong>{{ current.controls.climateTemperatureC }} °C</strong></div>
                            <div class="status-tile">Ventilador / modo<strong>{{ current.controls.climateFanSpeed }} · {{ current.controls.climateMode }}</strong></div>
                        </div>
                        @if (current.vehicle.energyPercent <= 10) {
                            <div class="mt-5 rounded-xl bg-amber-900/70 p-4 font-semibold text-amber-100">
                                Aviso: energía restante igual o inferior al 10%.
                            </div>
                        }
                    </div>
                } @else {
                    <div class="rounded-3xl bg-slate-900 p-6 text-slate-200">
                        <p>{{ message() }}</p>
                        @if (!paired()) {
                            <p class="mt-3 text-sm text-slate-400">
                                Solicita al operador un QR de emparejamiento para este vehículo.
                            </p>
                        }
                    </div>
                }
                <p class="mt-4 text-sm text-slate-500">{{ connection() }}</p>
                <p class="mt-6 text-xs text-slate-500">
                    Estado simulado por el backend. Esta pantalla no controla actuadores del vehículo.
                </p>
            </section>
        </main>
    `,
    styles: [`
        .status-tile { border-radius: 0.75rem; background: #1e293b; padding: 0.75rem; color: #cbd5e1; font-size: 0.875rem; }
        .status-tile strong { display: block; margin-top: 0.25rem; color: white; font-size: 1rem; }
    `]
})
export class VehicleDisplayComponent {
    private readonly http = inject(HttpClient);
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly destroyRef = inject(DestroyRef);
    private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));
    readonly state = signal<VehicleDisplayState | null>(null);
    readonly message = signal('Esperando vinculación con el vehículo.');
    readonly connection = signal('Sin conexión');
    readonly paired = signal(false);
    private readonly tokenKey = 'ecargo.vehicle.device-token';

    constructor() {
        if (!this.isBrowser) {
            return;
        }
        this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe(params => {
            const pairingCode = params.get('pairingCode');
            if (pairingCode) {
                this.claimDevice(pairingCode);
                void this.router.navigate([], { relativeTo: this.route, queryParams: {}, replaceUrl: true });
            }
        });

        timer(0, 2000).pipe(
            exhaustMap(() => {
                const token = localStorage.getItem(this.tokenKey);
                if (!token) {
                    this.paired.set(false);
                    this.message.set('Este dispositivo aún no está vinculado.');
                    return EMPTY;
                }
                this.paired.set(true);
                return this.http.get<VehicleDisplayState>(`${this.apiBaseUrl()}/api/v1/device/state`, {
                    headers: new HttpHeaders({ 'X-Vehicle-Device-Token': token })
                }).pipe(catchError(error => {
                    if (error.status === 401) {
                        localStorage.removeItem(this.tokenKey);
                        this.paired.set(false);
                        this.state.set(null);
                        this.message.set('La credencial fue rechazada. Vincula de nuevo con un código vigente.');
                        this.connection.set('Pantalla sin vincular');
                    } else {
                        this.connection.set(`Sin conexión (${error.status || 'red'}). Reintentando…`);
                    }
                    return EMPTY;
                }));
            }),
            takeUntilDestroyed()
        ).subscribe(value => {
            this.state.set(value);
            this.connection.set(`Conectado · Vehículo ${value.vehicle.vehicleId} · Actualización cada 2 s`);
        });
    }

    onOff(value: boolean): string {
        return value ? 'Activo' : 'Apagado';
    }

    private claimDevice(pairingCode: string): void {
        this.message.set('Vinculando dispositivo de solo lectura…');
        this.http.post<DeviceClaim>(`${this.apiBaseUrl()}/api/v1/device-enrollments/claim`, {
            pairingCode,
            deviceName: this.deviceName()
        }).pipe(
            switchMap(claim => {
                localStorage.setItem(this.tokenKey, claim.deviceToken);
                this.paired.set(true);
                return of(claim);
            }),
            catchError(error => {
                this.message.set(`No se pudo vincular el dispositivo (HTTP ${error.status || 'red'}). Solicita un QR vigente.`);
                return EMPTY;
            }),
            takeUntilDestroyed(this.destroyRef)
        ).subscribe(() => this.message.set('Dispositivo vinculado. Cargando estado…'));
    }

    private deviceName(): string {
        const platform = navigator.userAgent.includes('iPhone') ? 'iPhone'
            : navigator.userAgent.includes('Android') ? 'Android' : 'PWA';
        return `E-Cargo ${platform}`;
    }

    private apiBaseUrl(): string {
        if (environment.production) {
            return environment.apiUrl;
        }
        const backendUrl = new URL(environment.apiUrl);
        if (backendUrl.hostname === 'localhost' || backendUrl.hostname === '127.0.0.1') {
            backendUrl.hostname = location.hostname;
        }
        return backendUrl.origin;
    }
}
