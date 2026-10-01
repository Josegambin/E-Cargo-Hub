import { Component, OnInit, inject, DestroyRef } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { WebSocketService } from './core/services/websocket.facade';
import { VehicleStateService } from './core/services/vehicle-state.facade';

@Component({
    selector: 'app-root',
    standalone: true,
    imports: [RouterOutlet], 
    templateUrl: './app.component.html',
    styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {

    private ws = inject(WebSocketService);
    private state = inject(VehicleStateService);
    private destroyRef = inject(DestroyRef);

    ngOnInit(): void {
        this.ws.connect();
        this.ws.subscribeToFleetEvents();

        this.ws.telemetry$
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(telemetry => this.state.update(telemetry));

        this.ws.fleetEvents$
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(event => console.log('📢 Evento de flota:', event));
    }
}