import { Routes } from '@angular/router';
import { LayoutComponent } from './shared/components/layout/layout.component';

export const routes: Routes = [
    {
        path: '',
        component: LayoutComponent,
        children: [
            {
                path: 'dashboard',
                loadComponent: () => import('./features/dashboard/dashboard.component')
                    .then(m => m.DashboardComponent)
            },
            {
                path: 'vehicles',
                loadComponent: () => import('./features/vehicles/vehicles.component')
                    .then(m => m.VehiclesComponent)
            },
            {
                path: 'commands',
                loadComponent: () => import('./features/commands/command.component')
                    .then(m => m.CommandsComponent)
            },
            {
                path: 'telemetry',
                loadComponent: () => import('./features/telemetry/telemetry.component')
                    .then(m => m.TelemetryComponent)
            },
            {
                path: '',
                redirectTo: 'dashboard',
                pathMatch: 'full'
            }
        ]
    },
    {
        path: '**',
        redirectTo: 'dashboard'
    }
];